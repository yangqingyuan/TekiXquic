package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XWebSocket
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicLongNative
import com.lizhi.component.net.xquic.native.XquicMsgType
import com.lizhi.component.net.xquic.utils.XLogUtils
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
    pingInterval: Long
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

    private val xResponse: XResponse

    private val messageQueue = ArrayDeque<Message>()


    private fun threadFactory(name: String?, daemon: Boolean): ThreadFactory {
        return ThreadFactory { runnable ->
            val result = Thread(runnable, name)
            result.isDaemon = daemon
            result
        }
    }

    init {
        val name = "OkHttp WebSocket " + xRequest.url
        this.executor = ScheduledThreadPoolExecutor(2, threadFactory(name, false))
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
    }

    class PingRunnable(private val xRealWebSocket: XRealWebSocket) : Runnable {
        override fun run() {
            xRealWebSocket.sendPing("pong")
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

        headers.putAll(xRequest.headers.build().headersMap)
        return headers
    }

    fun connect(xquicClient: XquicClient) {
        executor.execute {
            XLogUtils.error("connect start")
            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url())
                .setToken(XRttInfoCache.tokenMap[url()])
                .setSession(XRttInfoCache.sessionMap[url()])
                .setTimeOut(xquicClient.connectTimeOut)
                .setMaxRecvLenght(1024 * 1024)
                .setCCType(xquicClient.ccType)

            sendParamsBuilder.setHeaders(parseHttpHeads())

            clientCtx = xquicLongNative.connect(sendParamsBuilder.build(), this)
            xquicLongNative.start(clientCtx)
            XLogUtils.error("connect end")
        }
    }

    private fun writeOneFrame() {
        synchronized(this) {
            if (!check()) {
                return
            }
            try {
                val msg = messageQueue.poll()
                if (msg == null) {
                    XLogUtils.error("msg is null")
                    return
                }
                when (msg.msgType) {
                    Message.MSG_TYPE_SEND -> {
                        xquicLongNative.send(clientCtx, msg.msgContent)
                        synchronized(this) { queueSize -= msg.msgContent.length }
                    }

                    Message.MSG_TYPE_CLOSE -> {
                        messageQueue.clear()
                        xquicLongNative.cancel(clientCtx)
                    }
                    else -> {
                        XLogUtils.error("unKnow message type")
                    }
                }
            } catch (e: java.lang.Exception) {
                XLogUtils.error(e)
            }
        }
    }

    class Message(var msgType: Int, var msgContent: String) {
        companion object {
            const val MSG_TYPE_SEND = 0
            const val MSG_TYPE_CLOSE = 1
        }
    }

    private fun check(): Boolean {
        if (clientCtx <= 0 || failed) {
            listener.onFailure(this, Exception("web socket is closed"), xResponse)
            return false
        }
        return true
    }

    override fun send(data: String): Boolean {
        synchronized(this) {
            // Don't send new frames after we've failed or enqueued a close frame.
            if (failed || enqueuedClose) return false

            // If this frame overflows the buffer, reject it and close the web socket.
            if (queueSize + data.length > MAX_QUEUE_SIZE) {
                close()
                return false
            }

            queueSize += data.length
            messageQueue.add(Message(Message.MSG_TYPE_SEND, data))
            writeOneFrame()
            return true
        }
    }

    private fun sendPing(pingContent: String) {
        synchronized(this) {
            if (!check()) return
            //xquicLongNative.sendPing(clientCtx, pingContent)
        }
    }

    private fun close(): Boolean {
        synchronized(this) {
            if (failed || enqueuedClose) return false
            enqueuedClose = true

            // Enqueue the close frame.
            messageQueue.add(
                Message(
                    Message.MSG_TYPE_CLOSE, "close"
                )
            )
            writeOneFrame()
            executor.shutdown()
            return true
        }
    }


    override fun cancel() {
        close()
    }

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
                XquicMsgType.PING.ordinal -> {
                    XLogUtils.debug("ping callback =" + String(data))
                }
                XquicMsgType.DESTROY.ordinal -> {
                    synchronized(this) {
                        clientCtx = 0
                    }
                }
                else -> {
                    XLogUtils.error("un know callback msg")
                }
            }
        }
    }

}