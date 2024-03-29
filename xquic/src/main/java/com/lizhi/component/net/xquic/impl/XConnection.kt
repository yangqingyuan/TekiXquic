package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.listener.XWebSocket
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XRequestBody
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.quic.Message
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception
import java.util.ArrayDeque
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 作用: 链接
 * 作者: yqy
 * 创建日期: 2022/5/26.
 */
class XConnection(
    private val xquicClient: XquicClient,
    private val originalRequest: XRequest,
    var tag: String? = null,
    var callBack: XCallBack? = null
) {

    companion object {
        private const val TAG = "XConnection"
    }

    private val authority = originalRequest.url.authority
    private var xWebSocket: XWebSocket? = null

    private var xCallBackMap: MutableMap<String, XCallBack?> = ConcurrentHashMap()

    @Volatile
    var idleAtNanos = Long.MAX_VALUE

    @Volatile
    var isDestroy = false

    /**
     * copy a newObject
     */
    var xRequest: XRequest = originalRequest

    internal val emptyXCall = object : XCall {
        override fun request(): XRequest {
            return xRequest
        }

        override fun enqueue(xCallback: XCallBack?) {
        }

        override fun cancel() {
        }

        override fun isExecuted(): Boolean {
            return false
        }

        override fun isCanceled(): Boolean {
            return false
        }
    }

    /**
     * 存储还未链接的数据
     */
    private val messageQueue = ArrayDeque<Message>()

    init {
        tag?.let {
            callBack?.let { back ->
                xCallBackMap[it] = back
                originalRequest.setUerTag(it)//FIXME 这里是使用临时tag作为区分不同请求的callback，比较混乱
            }
        }
        xquicClient.newWebSocket(originalRequest, object : XWebSocketListener {
            override fun onOpen(webSocket: XWebSocket, response: XResponse) {
                XLogUtils.debug(TAG, "onOpen")
                xWebSocket = webSocket
                while (true) {
                    val content = messageQueue.poll() ?: return
                    webSocket.send(content)
                }
            }

            override fun onMessage(webSocket: XWebSocket, response: XResponse, isFinish: Boolean) {
                synchronized(this) {
                    idleAtNanos = System.nanoTime()
                    val tag = response.xResponseBody.tag
                    xCallBackMap[tag]?.onResponse(emptyXCall, response, isFinish)
                    xCallBackMap.remove(tag)//to free Reference
                }
            }

            override fun onClosed(webSocket: XWebSocket, code: Int, reason: String?) {
                callBackAndReleaseData(Exception("connect closed : code=$code,reason=$reason"))
            }

            override fun onFailure(webSocket: XWebSocket, t: Throwable, response: XResponse) {
                callBackAndReleaseData(t)
            }
        })
    }

    private fun callBackAndReleaseData(t: Throwable) {
        synchronized(this) {
            xWebSocket = null
            isDestroy = true
            idleAtNanos = 0
            xquicClient.connectionPool().remove(this@XConnection)//clean conn
            XLogUtils.debug(TAG, "onFailure")
            val iterator = xCallBackMap.iterator()
            while (iterator.hasNext()) {
                val callBack = iterator.next()
                callBack.value?.onFailure(emptyXCall, Exception(t))
            }
            xCallBackMap.clear()//to free Reference
        }
    }

    @Synchronized
    fun send(
        tag: String, body: XRequestBody, headers: HashMap<String, String>, xCallBack: XCallBack?
    ) {
        if (isDestroy) {
            xCallBack?.onFailure(emptyXCall, Exception("connection is destroy"))
            return
        }
        body.let {
            xCallBackMap[tag] = xCallBack //FIXME 这里是使用临时tag作为区分不同请求的callback，比较混乱
            val message = Message.makeMessageByReqBody(body, headers, tag)
            if (xWebSocket != null) {
                xWebSocket?.send(message)
            } else {//如果没有链接，开始链接,并将参数缓存起来，握手成功后再发送信息
                messageQueue.add(message)
            }
        }
    }


    /**
     * 是否符合条件
     */
    fun isEligible(xRequest: XRequest): Boolean {
        if (authority == xRequest.url.authority && !isDestroy) {//通过域名来判断是否符合
            return true
        }
        return false
    }

    /**
     * 取消的时候移除监听
     */
    fun cancel(tag: String) {
        synchronized(this) {
            xCallBackMap.remove(tag)
        }
    }

    fun close(code: Int = 0, reason: String? = null) {
        xWebSocket?.close(code, reason)
    }
}