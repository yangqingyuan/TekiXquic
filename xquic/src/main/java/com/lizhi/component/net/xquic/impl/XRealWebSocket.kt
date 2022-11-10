package com.lizhi.component.net.xquic.impl

import android.os.Build
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.*
import com.lizhi.component.net.xquic.mode.*
import com.lizhi.component.net.xquic.quic.*
import com.lizhi.component.net.xquic.utils.XLogUtils
import org.json.JSONObject
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit


/**
 * 作用: 长链接
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRealWebSocket(
    private val xRequest: XRequest,
    private val listener: XWebSocketListener,
    private val xRttInfoCache: XRttInfoListener,
    private val random: Random,
    private val pingInterval: Long,
    private val pingListener: XPingListener,
    private val pingTimeOutCount: Int
) : XWebSocket, XquicCallback {

    companion object {

        private const val STATUS_CLOSE = 2
        private const val STATUS_CANCEL = 1

        private const val MAX_BUFF_SIZE = 1024 * 1024

        private const val MAX_QUEUE_SIZE = (16 * 1024 * 1024).toLong() // 16 MiB.

        /**
         * check clientCtx Valid
         */
        internal fun checkClientCtx(clientCtx: Long): Boolean {
            if (clientCtx != 0L && clientCtx != -1L) {
                return true
            }
            return false
        }
    }

    private val xquicLongNative: XquicLongNative = XquicLongNative()
    private var executor: ScheduledExecutorService
    private val key: String

    /**
     * is callback fail
     */
    @Volatile
    private var failed = false

    @Volatile
    private var queueSize: Long = 0

    @Volatile
    private var clientCtx: Long = 0L

    /**
     * when close enqueuedClose is true
     */
    @Volatile
    private var enqueuedClose = false

    private val xResponse: XResponse

    /**
     * use to cache message
     */
    private val messageQueue = ArrayDeque<Message>()

    /**
     * real to send runnable
     */
    private val writerRunnable: Runnable

    /**
     * user default close code ,will return at onClosed
     */
    private var code: Int = -1

    /**
     * user default close reason ,will return at onClosed
     */
    private var reason: String? = null

    /**
     * alpn type
     */
    private var alpnType = AlpnType.ALPN_H3

    @Volatile
    private var cancelOrClose = 0// 2 cancel 1:close

    /**
     * isCallback
     */
    var isCallback = false

    /**
     * Shared memory is more efficient,default 1M
     */
    private var byteBuffer = ByteBuffer.allocateDirect(MAX_BUFF_SIZE)

    /**
     * Whether the sign network is changed
     */
    private var netHashCode: Int = -1

    /**
     * current ping time out count
     */
    @Volatile
    private var currentPingTimeOutCount = 0

    private fun threadFactory(): ThreadFactory {
        return ThreadFactory { runnable ->
            val result = Thread(runnable, "OkHttp WebSocket " + xRequest.url)
            result.isDaemon = false
            result
        }
    }

    init {
        this.executor = ScheduledThreadPoolExecutor(2, threadFactory())
        if (pingInterval > 0) {
            executor.scheduleAtFixedRate(
                PingRunnable(this),
                pingInterval,
                pingInterval,
                TimeUnit.MILLISECONDS
            )
        }
        val nonce = ByteArray(16)
        random.nextBytes(nonce)
        this.key = String(nonce)

        xResponse = XResponse.Builder()
            .headers(xRequest.headers)
            .request(xRequest)
            .build()

        /**
         * writer Runnable
         */
        writerRunnable = Runnable {
            synchronized(this) {
                while (writeOneFrame()) {
                }
            }
        }
    }

    private fun runWriter() {
        if (!check()) {
            return
        }
        synchronized(this) {
            executor.execute(writerRunnable)
        }
    }

    /**
     * send ping
     */
    class PingRunnable(private val xRealWebSocket: XRealWebSocket) : Runnable {
        override fun run() {
            if (!checkClientCtx(xRealWebSocket.clientCtx) || xRealWebSocket.failed || xRealWebSocket.enqueuedClose) return

            //if ping time out
            if (xRealWebSocket.pingTimeOutCount > 0) {
                if (xRealWebSocket.currentPingTimeOutCount >= xRealWebSocket.pingTimeOutCount) {
                    xRealWebSocket.close(
                        -1,
                        "ping time out count:${xRealWebSocket.pingTimeOutCount}"
                    )
                    return
                }
            }

            //if network change
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (xRealWebSocket.netHashCode != -1 && (xRealWebSocket.netHashCode != XNetStatusCallBack.netHashCode)) {
                    if (xRealWebSocket.isCallback) {
                        return
                    }
                    synchronized(xRealWebSocket.isCallback) {
                        if (!xRealWebSocket.isCallback) {
                            xRealWebSocket.close(
                                -1,
                                "network is changed, Connection migration is not supported"
                            )
                            xRealWebSocket.isCallback = true
                        }
                    }
                    return
                }
                xRealWebSocket.netHashCode = XNetStatusCallBack.netHashCode
            }

            var pingBody = xRealWebSocket.pingListener.ping()
            if (pingBody.size > 256) {
                pingBody = "ping body ti too lang".toByteArray()
            }
            xRealWebSocket.xquicLongNative.sendPing(
                xRealWebSocket.clientCtx,
                pingBody,
                pingBody.size
            )
        }
    }

    private fun authority(): String {
        return xRequest.url.authority
    }

    /**
     * parse http heads
     * more headers "https://zhuanlan.zhihu.com/p/282737965"
     */
    private fun parseHttpHeads(): HashMap<String, String> {
        val headers = hashMapOf<String, String>()
        if (alpnType == AlpnType.ALPN_H3) {
            /* set headers */
            headers["Upgrade"] = "websocket"
            headers["Connection"] = "Upgrade"
            headers["Sec-WebSocket-Key"] = "key"
            headers["Sec-WebSocket-Version"] = "13"

            /* set headers */
            headers[":method"] = xRequest.method
            headers[":scheme"] = xRequest.url.scheme
            headers[":authority"] = xRequest.url.authority
            xRequest.url.path?.let {
                headers[":path"] = it
            }
        }
        headers.putAll(xRequest.headers.headersMap)
        return headers
    }

    /**
     * （1）send content
     * （2）data type
     * （3）contentLength
     */
    private fun setContent(sendParamsBuilder: SendParams.Builder, xRequest: XRequest) {
        xRequest.body?.let {
            val message: Message = Message.makeMessageByReqBody(it, null, xRequest.userTag() ?: "")
            sendParamsBuilder.setDataType(DataType.getDataTypeByMediaType(it.mediaType))
            sendParamsBuilder.setContent(message.getContent())
            sendParamsBuilder.setContentLength(message.getContentLength())
        }
    }

    fun connect(xquicClient: XquicClient) {
        if (clientCtx > 0) {
            XLogUtils.warn("is connect ")
            return
        }
        XLogUtils.debug("=======> connect start <========")
        executor.execute {

            try {
                val url = xRequest.url.getHostUrl(xquicClient.dns)
                if (url == null) {
                    listener.onFailure(
                        this,
                        Exception("dns can not parse domain ${xRequest.url.url} error"),
                        xResponse
                    )
                    return@execute
                }
                alpnType = xquicClient.alpnType

                val sendParamsBuilder = SendParams.Builder()
                    .setUrl(url)
                    .setToken(xRttInfoCache.getToken(authority()))
                    .setSession(xRttInfoCache.getSession(authority()))
                    .setConnectTimeOut(xquicClient.connectTimeOut)
                    .setReadTimeOut(xquicClient.readTimeout)
                    .setMaxRecvLenght(xquicClient.maxRecvDataLen)
                    .setCCType(xquicClient.ccType)
                    .setCryptoFlag(xquicClient.cryptoFlag)
                    .setFinishFlag(xquicClient.finishFlag)
                    .setProtoVersion(xquicClient.protoVersion)
                    .setAlpnType(alpnType)

                sendParamsBuilder.setHeaders(parseHttpHeads())

                //Content can be set before connection, supporting 0rtt if have session
                setContent(sendParamsBuilder, xRequest)

                clientCtx = xquicLongNative.connect(sendParamsBuilder.build(), this)
                if (!checkClientCtx(clientCtx)) {
                    listener.onFailure(this, java.lang.Exception("connect error"), xResponse)
                } else {
                    /* 注意：这里是阻塞的 */
                    xquicLongNative.start(clientCtx)
                }
                clientCtx = 0
                /* 注意：阻塞结束说明已经内部已经结束了 */
                if (!executor.isShutdown) {
                    executor.shutdownNow()
                }

                if (cancelOrClose == STATUS_CLOSE) {
                    listener.onClosed(this, code, reason)
                } else if (cancelOrClose <= STATUS_CANCEL) {
                    listener.onFailure(this, Throwable("cancel or exception"), xResponse)
                }

            } catch (e: Exception) {
                XLogUtils.error(e)
            } finally {
                XLogUtils.debug("=======> execute end <========")
            }
        }
    }

    private fun writeOneFrame(): Boolean {
        try {
            val msg = messageQueue.poll() ?: return false
            when (msg.msgType) {
                Message.MSG_TYPE_SEND -> {//
                    if (checkClientCtx(clientCtx) && !failed && !enqueuedClose) {
                        byteBuffer.clear()//notice clear data
                        byteBuffer.put(msg.getContent()) //use byte buffer to Memory sharing
                        when (xquicLongNative.sendByte(
                            clientCtx,
                            msg.dataType,
                            byteBuffer,
                            msg.getContentLength()
                        )) {
                            XquicCallback.XQC_OK -> {
                                queueSize -= msg.getContentLength()
                            }
                            else -> {
                                listener.onFailure(
                                    this,
                                    java.lang.Exception("connect is close"),
                                    xResponse
                                )
                            }
                        }
                    }
                }

                Message.MSG_TYPE_CLOSE -> {//close
                    enqueuedClose = true
                    messageQueue.clear()
                    if (checkClientCtx(clientCtx)) {
                        xquicLongNative.cancel(clientCtx)
                    }
                    clientCtx = 0
                    failed = true
                    if (!executor.isShutdown) {
                        executor.shutdownNow()
                    }
                    return false
                }
                else -> {
                    XLogUtils.error("unKnow message type")
                }
            }
            return true
        } catch (e: java.lang.Exception) {
            XLogUtils.error(e)
        }
        return false
    }


    /**
     * check
     */
    private fun check(): Boolean {
        if (!checkClientCtx(clientCtx) || failed || enqueuedClose) {
            listener.onFailure(this, Exception("web socket is closed"), xResponse)
            return false
        }
        return true
    }


    /**
     * send data
     */
    @Synchronized
    override fun send(data: String): Boolean {
        return send(data, "")
    }

    @Synchronized
    override fun send(data: String, tag: String): Boolean {
        // Don't send new frames after we've failed or enqueued a close frame.
        if (!check()) return false

        // If this frame overflows the buffer, reject it and close the web socket.
        if (queueSize + data.length > MAX_QUEUE_SIZE) {
            close()
            return false
        }

        queueSize += data.length
        messageQueue.add(Message.makeJsonMessage(data, tag))

        runWriter()
        return true
    }

    @Synchronized
    override fun send(byteArray: ByteArray): Boolean {
        // Don't send new frames after we've failed or enqueued a close frame.
        if (!check()) return false

        if (byteArray.size > MAX_BUFF_SIZE) {
            XLogUtils.error("send error : byte size is too long")
            throw BufferOverflowException()
        }
        queueSize += byteArray.size
        messageQueue.add(Message.makeByteMessage(byteArray))
        runWriter()
        return true
    }

    @Synchronized
    override fun send(message: Message): Boolean {
        if (!check()) return false

        // If this frame overflows the buffer, reject it and close the web socket.
        if (queueSize + message.getContentLength() > MAX_QUEUE_SIZE) {
            close()
            return false
        }
        queueSize += message.getContentLength()
        messageQueue.add(message)
        runWriter()
        return true
    }


    /**
     * close
     */
    @Synchronized
    private fun close(): Boolean {
        if (failed || enqueuedClose) return false
        // Enqueue the close frame.
        messageQueue.add(Message.makeCloseMessage())
        runWriter()
        return true
    }


    /**
     * result on : onFailed
     * 跟close的唯一区别是cancel会在onFailed中返回
     */
    override fun cancel() {
        cancelOrClose = STATUS_CANCEL
        close()
    }

    /**
     * result on : onClose
     * 跟cancel的唯一区别是close会在onClose，将传递的参数逐一返回
     */
    override fun close(code: Int, reason: String?) {
        this.code = code
        this.reason = reason
        cancelOrClose = STATUS_CLOSE
        close()
    }

    /**
     * 判断是否已经关闭
     */
    override fun isClose(): Boolean {
        if (failed || enqueuedClose || !checkClientCtx(clientCtx)) return true
        return false
    }

    /**
     * callback data
     */
    @Synchronized
    override fun callBackData(ret: Int, tag: String?, data: ByteArray) {
        if (ret == XquicCallback.XQC_OK) {
            xResponse.xResponseBody = XResponseBody(data, tag)
            listener.onMessage(this, xResponse)
        } else {
            if (isCallback) {
                XLogUtils.warn(
                    "is callback on need to callback again!! ret=${ret},data=${String(data)}"
                )
                return
            }
            synchronized(isCallback) {
                if (!isCallback) {
                    close(ret, String(data))
                    isCallback = true
                }
            }
        }
    }

    /**
     * call back message
     */
    override fun callBackMessage(msgType: Int, data: ByteArray) {
        synchronized(this) {
            when (msgType) {
                XquicMsgType.HANDSHAKE -> {
                    listener.onOpen(this, xResponse)
                }
                XquicMsgType.TOKEN -> {
                    xRttInfoCache.tokenBack(authority(), data)
                }
                XquicMsgType.SESSION -> {
                    xRttInfoCache.sessionBack(authority(), data)
                }
                XquicMsgType.TP -> {

                }
                XquicMsgType.HEAD -> {
                    try {
                        val headJson = JSONObject(String(data))
                        val xHeaderBuild = XHeaders.Builder()
                        for (key in headJson.keys()) {
                            xHeaderBuild.add(key, headJson.getString(key))
                        }
                        xResponse.xHeaders = xHeaderBuild.build()
                    } catch (e: java.lang.Exception) {
                        XLogUtils.error(e)
                    }
                }
                XquicMsgType.PING -> {
                    pingListener.pong(data)
                }
                XquicMsgType.DESTROY -> {
                    close()
                }
                else -> {
                    XLogUtils.error("un know callback msg")
                }
            }
        }
    }

}