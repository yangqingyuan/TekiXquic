package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.mode.XResponseBody
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicMsgType
import com.lizhi.component.net.xquic.native.XquicShortNative
import com.lizhi.component.net.xquic.native.XquicShortNative.SendParams
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.io.InterruptedIOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException


/**
 * 作用: 异步调用
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XAsyncCall(
    var xCall: XCall,
    var xquicClient: XquicClient,
    var originalRequest: XRequest,
    var responseCallback: XCallBack? = null
) : XNamedRunnable(), XquicCallback {

    companion object {

        /**
         * token
         */
        val tokenMap by lazy { hashMapOf<String, String>() }

        /**
         * session
         */
        val sessionMap by lazy { hashMapOf<String, String>() }

        /**
         * tp
         */
        val tpMap by lazy { hashMapOf<String, String>() }

        var index = 0
    }

    init {
        name = String.format(Locale.US, "${XLogUtils.commonTag} %s", originalRequest.url)
    }

    fun executeOn(executorService: ExecutorService?) {
        assert(!Thread.holdsLock(xquicClient.dispatcher()))
        var success = false
        try {
            executorService!!.execute(this)
            success = true
        } catch (e: RejectedExecutionException) {
            val ioException = InterruptedIOException("executor rejected")
            ioException.initCause(e)
        } finally {
            if (!success) {
                xquicClient.dispatcher().finished(this) // This call is no longer running!
            }
        }
    }

    fun url(): String {
        return originalRequest.url.url!!
    }

    fun request(): XRequest {
        return originalRequest
    }

    override fun execute() {
        val startTime = System.currentTimeMillis()
        try {
            XLogUtils.debug("=======> execute start <========")
            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url())
                .setToken(tokenMap[url()])
                .setSession(sessionMap[url()])
                .setTimeOut(xquicClient.connectTimeOut)
                .setMaxRecvLenght(1024 * 1024)
                .setAuthority(xquicClient.authority)
                .setCCType(xquicClient.ccType)

            /* set headers */
            val headers = hashMapOf<String, String>()
            headers[":method"] = originalRequest.method
            headers.putAll(originalRequest.headers.build().headersMap)
            if (originalRequest.method == "POST") {
                val body = originalRequest.body

                headers["content-type"] = body.mediaType.mediaType
                headers["content-length"] = body.content.length.toString()
                sendParamsBuilder.setContent(body.content)
            }
            sendParamsBuilder.setHeaders(headers)

            /* native to send */
            XquicShortNative().send(
                sendParamsBuilder.build(), this
            )
        } catch (e: Exception) {
            cancel()
        } finally {
            XLogUtils.debug("=======> execute end cost(${System.currentTimeMillis() - startTime} ms),index(${index})<========")
            index += 1
            xquicClient.dispatcher().finished(this)
        }

    }

    override fun callBackReadData(ret: Int, data: ByteArray) {
        if (ret == 0) {
            val xResponse = XResponse.Builder()
                .headers(originalRequest.headers.build())
                .responseBody(XResponseBody(data))
                .request(originalRequest)
                .build()
            xResponse.code = ret
            responseCallback?.onResponse(xCall, xResponse)
        } else {
            val errMsg = String(data)
            responseCallback?.onFailure(xCall, Exception(errMsg))
        }
    }

    override fun callBackMessage(msgType: Int, data: ByteArray) {
        XLogUtils.debug("callBackMessage msgType=$msgType")

        synchronized(this) {
            when (msgType) {
                XquicMsgType.TOKEN.ordinal -> {
                    tokenMap[url()] = String(data)
                }
                XquicMsgType.SESSION.ordinal -> {
                    sessionMap[url()] = String(data)
                }
                XquicMsgType.TP.ordinal -> {
                    tpMap[url()] = String(data)
                }
            }
        }

    }

    fun get(): XAsyncCall {
        return this
    }

    fun cancel() {
        XLogUtils.info("cancel")
    }
}