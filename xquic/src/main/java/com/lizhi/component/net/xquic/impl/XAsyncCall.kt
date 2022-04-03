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
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.io.InterruptedIOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

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

    fun host(): String {
        return originalRequest.url
    }

    fun request(): XRequest {
        return originalRequest
    }

    override fun execute() {
        XLogUtils.debug("=======> execute <========")

        val sendParams = XquicShortNative.SendParams.Builder()
            .setUrl(host())
            .setToken(tokenMap[host()])
            .setSession(sessionMap[host()])
            .setContent("demo/tile")
            .setTimeOut(xquicClient.connectTimeOut)
            .setMethod(originalRequest.method)
            .setMaxRecvLenght(1024 * 1024)
            .setAuthority(xquicClient.authority)
            .setCCType(xquicClient.ccType)
            .build()

        XquicShortNative().send(
            sendParams, this
        )
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
                    tokenMap[host()] = String(data)
                }
                XquicMsgType.SESSION.ordinal -> {
                    sessionMap[host()] = String(data)
                }
                XquicMsgType.TP.ordinal -> {
                    tpMap[host()] = String(data)
                }
            }
        }

    }
}