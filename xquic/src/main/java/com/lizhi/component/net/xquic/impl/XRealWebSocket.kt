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
import java.lang.Exception
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory

/**
 * 作用: 长链接
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRealWebSocket(
    private val xRequest: XRequest,
    private val listener: XWebSocketListener,
    random: Random,
    private val pingInterval: Int
) : XWebSocket, XquicCallback {

    private var xquicLongNative: XquicLongNative = XquicLongNative()
    private val executor: ScheduledExecutorService
    private val key: String

    private fun threadFactory(name: String?, daemon: Boolean): ThreadFactory {
        return ThreadFactory { runnable ->
            val result = Thread(runnable, name)
            result.isDaemon = daemon
            result
        }
    }

    init {
        val name = "OkHttp WebSocket " + xRequest.url
        this.executor = ScheduledThreadPoolExecutor(1, threadFactory(name, false))
        val nonce = ByteArray(16)
        random.nextBytes(nonce)
        this.key = String(nonce)
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
            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url())
                .setToken(XRttInfoCache.tokenMap[url()])
                .setSession(XRttInfoCache.sessionMap[url()])
                .setTimeOut(xquicClient.connectTimeOut)
                .setMaxRecvLenght(1024 * 1024)
                .setCCType(xquicClient.ccType)

            sendParamsBuilder.setHeaders(parseHttpHeads())

            xquicLongNative.clientCtx = xquicLongNative.connect(sendParamsBuilder.build(), this)
            xquicLongNative.start(xquicLongNative.clientCtx)
        }
    }


    override fun send(text: String) {
        synchronized(this) {
            if (xquicLongNative.clientCtx <= 0) {
                return
            }
            xquicLongNative.send(xquicLongNative.clientCtx, text)
        }
    }

    override fun cancel() {
        synchronized(this) {
            if (xquicLongNative.clientCtx <= 0) {
                return
            }
            xquicLongNative.cancel(xquicLongNative.clientCtx)
            xquicLongNative.clientCtx = 0
        }
    }

    override fun callBackData(ret: Int, data: ByteArray) {
        synchronized(this) {
            if (ret == XquicCallback.XQC_OK) {
                listener.onMessage(this, data)
            } else {
                val errMsg = String(data)
                val xResponse = XResponse.Builder()
                    .headers(xRequest.headers.build())
                    .request(xRequest)
                    .build()
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

                XquicMsgType.DESTROY.ordinal -> {
                    synchronized(this) {
                        xquicLongNative.clientCtx = 0
                    }
                }
                else -> {
                    XLogUtils.error("un know callback msg")
                }
            }
        }
    }

}