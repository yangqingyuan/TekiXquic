package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XPingListener
import com.lizhi.component.net.xquic.listener.XWebSocket
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XHeaders
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicLongNative
import com.lizhi.component.net.xquic.native.XquicMsgType
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
        private const val MAX_QUEUE_SIZE = (16 * 1024 * 1024 // 16 MiB.
                ).toLong()
    }

    private val xquicLongNative: XquicLongNative = XquicLongNative()
    private var executor: ScheduledExecutorService
    private val key: String
    private var failed = false
    private var queueSize: Long = 0
    private var clientCtx: Long = 0L
    private var enqueuedClose = false
    private var isWriterRunning = false

    private val xResponse: XResponse

    private val messageQueue = ArrayDeque<Message>()

    private val writerRunnable: Runnable

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
            .headers(xRequest.headers.build())
            .request(xRequest)
            .build()

        /**
         * writer Runnable
         */
        writerRunnable = Runnable {
            isWriterRunning = true
            while (writeOneFrame()) {
            }
        }
    }

    private fun runWriter() {
        assert(Thread.holdsLock(this))
        if (isWriterRunning) {
            if (!messageQueue.isEmpty()) {
                return
            }
        }
        executor.execute(writerRunnable)
    }

    /**
     * send ping
     */
    class PingRunnable(private val xRealWebSocket: XRealWebSocket) : Runnable {
        override fun run() {
            if (xRealWebSocket.clientCtx <= 0 || xRealWebSocket.failed || xRealWebSocket.enqueuedClose) return
            xRealWebSocket.xquicLongNative.sendPing(
                xRealWebSocket.clientCtx,
                xRealWebSocket.pingListener.ping()
            )
        }
    }

    fun url(): String {
        return xRequest.url.url
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

        headers.putAll(xRequest.headers.build().headersMap)
        return headers
    }

    fun connect(xquicClient: XquicClient) {
        if (clientCtx > 0) {
            XLogUtils.warn("is connect ")
            return
        }
        XLogUtils.debug("=======> connect start <========")
        executor.execute {
            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url())
                .setToken(XRttInfoCache.tokenMap[url()])
                .setSession(XRttInfoCache.sessionMap[url()])
                .setTimeOut(xquicClient.connectTimeOut)
                .setMaxRecvLenght(1024 * 1024)
                .setCCType(xquicClient.ccType)

            sendParamsBuilder.setHeaders(parseHttpHeads())

            clientCtx = xquicLongNative.connect(sendParamsBuilder.build(), this)

            /* 注意：这里是阻塞的 */
            xquicLongNative.start(clientCtx)

            /* 注意：阻塞结束说明已经内部已经结束了 */
            executor.shutdownNow()

            XLogUtils.debug("=======> execute end <========")
        }
    }

    private fun writeOneFrame(): Boolean {
        synchronized(this) {
            try {
                val msg = messageQueue.poll()
                if (msg == null) {
                    isWriterRunning = false
                    return false
                }
                when (msg.msgType) {
                    Message.MSG_TYPE_SEND -> {//
                        if (clientCtx > 0 && !failed && !enqueuedClose) {
                            when (xquicLongNative.send(clientCtx, msg.msgContent)) {
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
                        if (clientCtx > 0) {
                            xquicLongNative.cancel(clientCtx)
                        }
                        isWriterRunning = false
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
        }
        return false
    }

    /**
     * message object
     */
    class Message(var msgType: Int, var msgContent: String) {
        companion object {
            const val MSG_TYPE_SEND = 0
            const val MSG_TYPE_CLOSE = 1
        }
    }

    /**
     * check
     */
    private fun check(): Boolean {
        if (clientCtx <= 0 || failed || enqueuedClose) {
            listener.onFailure(this, Exception("web socket is closed"), xResponse)
            return false
        }
        return true
    }


    /**
     * send data
     */
    override fun send(data: String): Boolean {
        synchronized(this) {
            // Don't send new frames after we've failed or enqueued a close frame.
            if (!check()) return false

            // If this frame overflows the buffer, reject it and close the web socket.
            if (queueSize + data.length > MAX_QUEUE_SIZE) {
                close()
                return false
            }

            queueSize += data.length
            messageQueue.add(Message(Message.MSG_TYPE_SEND, data))

            runWriter()
            return true
        }
    }


    /**
     * close
     */
    private fun close(): Boolean {
        synchronized(this) {
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
    }


    /**
     * cancel conn
     */
    override fun cancel() {
        close()
    }

    /**
     * callback data
     */
    override fun callBackData(ret: Int, data: ByteArray) {
        synchronized(this) {
            if (ret == XquicCallback.XQC_OK) {
                listener.onMessage(this, data)
            } else {
                clientCtx = 0
                failed = true
                val errMsg = String(data)
                listener.onFailure(this, Exception(errMsg), xResponse)
            }
        }
    }

    /**
     * call back message
     */
    override fun callBackMessage(msgType: Int, data: ByteArray) {
        synchronized(this) {
            when (msgType) {
                XquicMsgType.TOKEN.ordinal -> {
                    XRttInfoCache.tokenMap.put(url(), String(data))
                }
                XquicMsgType.SESSION.ordinal -> {
                    XRttInfoCache.sessionMap.put(url(), String(data))
                }
                XquicMsgType.TP.ordinal -> {
                    XRttInfoCache.tpMap.put(url(), String(data))
                }
                XquicMsgType.HEAD.ordinal -> {
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
                XquicMsgType.PING.ordinal -> {
                    pingListener.pong(String(data))
                }
                XquicMsgType.DESTROY.ordinal -> {
                    close()
                }
                else -> {
                    XLogUtils.error("un know callback msg")
                }
            }
        }
    }

}