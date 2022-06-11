package com.lizhi.component.net.xquic.impl

import com.google.gson.Gson
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XPingListener
import com.lizhi.component.net.xquic.listener.XWebSocket
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XHeaders
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.mode.XResponseBody
import com.lizhi.component.net.xquic.native.*
import com.lizhi.component.net.xquic.utils.XLogUtils
import org.json.JSONObject
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
    random: Random,
    pingInterval: Long,
    private val pingListener: XPingListener
) : XWebSocket, XquicCallback {

    companion object {

        private const val STATUS_CLOSE = 1
        private const val STATUS_CANCEL = 2

        private const val MAX_QUEUE_SIZE = (16 * 1024 * 1024 // 16 MiB.
                ).toLong()

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

    @Volatile
    private var cancelOrClose = 0// 2 cancle 1:close

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
            var pingBody = xRealWebSocket.pingListener.ping()
            if (pingBody.length > 256) {
                pingBody = "ping body ti too lang"
            }
            xRealWebSocket.xquicLongNative.sendPing(
                xRealWebSocket.clientCtx,
                pingBody
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
        /* set headers */
        val headers = hashMapOf<String, String>()
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

        headers.putAll(xRequest.headers.headersMap)
        return headers
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

                val sendParamsBuilder = SendParams.Builder()
                    .setUrl(url)
                    .setToken(XRttInfoCache.tokenMap[authority()])
                    .setSession(XRttInfoCache.sessionMap[authority()])
                    .setConnectTimeOut(xquicClient.connectTimeOut)
                    .setReadTimeOut(xquicClient.readTimeout)
                    .setMaxRecvLenght(1024 * 1024)
                    .setCCType(xquicClient.ccType)

                sendParamsBuilder.setHeaders(parseHttpHeads())

                clientCtx = xquicLongNative.connect(sendParamsBuilder.build(), this)
                if (!checkClientCtx(clientCtx)) {
                    listener.onFailure(this, java.lang.Exception("connect error"), xResponse)
                } else {
                    /* 注意：这里是阻塞的 */
                    xquicLongNative.start(clientCtx)
                }

                /* 注意：阻塞结束说明已经内部已经结束了 */
                executor.shutdownNow()

                if (cancelOrClose == STATUS_CLOSE) {
                    listener.onClosed(this, code, reason)
                } else if (cancelOrClose == STATUS_CANCEL) {
                    listener.onFailure(this, Throwable("cancel"), xResponse)
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
                        when (xquicLongNative.send(clientCtx, msg.getContent())) {
                            XquicCallback.XQC_OK -> {
                                synchronized(this) { queueSize -= msg.msgContent.length }
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
     * message object
     */
    class Message(
        var msgType: Int = MSG_TYPE_SEND,
        var msgContent: String,
        var tag: String? = null,
        var header: HashMap<String, String>? = null
    ) {
        companion object {
            const val MSG_TYPE_SEND = 0
            const val MSG_TYPE_CLOSE = 1
            val gson = Gson()
        }

        private val sendBody = SendBody()

        init {
            sendBody.send_body = msgContent
            sendBody.user_tag = tag
            header?.forEach(action = {
                sendBody.headers.add(SendBody.Header(it.key, it.value))
            })
        }

        fun getContent(): String {
            return gson.toJson(sendBody)
        }
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
        messageQueue.add(Message(Message.MSG_TYPE_SEND, data, tag))

        runWriter()
        return true
    }

    override fun send(message: Message): Boolean {
        if (!check()) return false

        // If this frame overflows the buffer, reject it and close the web socket.
        if (queueSize + message.msgContent.length > MAX_QUEUE_SIZE) {
            close()
            return false
        }
        queueSize += message.msgContent.length
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
        messageQueue.add(
            Message(
                Message.MSG_TYPE_CLOSE, "close"
            )
        )
        runWriter()
        return true
    }


    /**
     * cancel conn
     */
    override fun cancel() {
        cancelOrClose = STATUS_CANCEL
        close()
    }

    override fun close(code: Int, reason: String?) {
        this.code = code
        this.reason = reason
        cancelOrClose = STATUS_CLOSE
        close()
    }

    /**
     * callback data
     */
    @Synchronized
    override fun callBackData(ret: Int, data: String) {
        if (ret == XquicCallback.XQC_OK) {
            xResponse.xResponseBody = XResponseBody(data)
            listener.onMessage(this, xResponse)
        } else {
            clientCtx = 0
            failed = true
            listener.onFailure(this, Exception(JSONObject(data).getString("recv_body")), xResponse)
        }
    }

    /**
     * call back message
     */
    override fun callBackMessage(msgType: Int, data: String) {
        synchronized(this) {
            when (msgType) {
                XquicMsgType.HANDSHAKE -> {
                    listener.onOpen(this, xResponse)
                }
                XquicMsgType.TOKEN -> {
                    XRttInfoCache.tokenMap.put(authority(), data)
                }
                XquicMsgType.SESSION -> {
                    XRttInfoCache.sessionMap.put(authority(), data)
                }
                XquicMsgType.TP -> {
                    XRttInfoCache.tpMap.put(authority(), data)
                }
                XquicMsgType.HEAD -> {
                    try {
                        val headJson = JSONObject(data)
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