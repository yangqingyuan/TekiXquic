package com.lizhi.component.net.xquic.impl

import android.util.Log
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.listener.XquicCallback
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.mode.XResponseBody
import com.lizhi.component.net.xquic.native.XquicShortNative
import java.io.InterruptedIOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

class XAsyncCall(
    var xCall: XCall,
    var xquicClient: XquicClient?,
    var originalRequest: XRequest?,
    var responseCallback: XCallBack? = null
) : XNamedRunnable(), XquicCallback {

    init {
        name = String.format(Locale.US, "Xquic %s", originalRequest?.url)
    }

    fun executeOn(executorService: ExecutorService?) {
        assert(!Thread.holdsLock(xquicClient?.dispatcher()))
        var success = false
        try {
            executorService!!.execute(this)
            success = true
        } catch (e: RejectedExecutionException) {
            val ioException = InterruptedIOException("executor rejected")
            ioException.initCause(e)
        } finally {
            if (!success) {
                xquicClient?.dispatcher()?.finished(this) // This call is no longer running!
            }
        }
    }

    fun host(): String? {
        return originalRequest?.url
    }

    fun request(): XRequest? {
        return originalRequest
    }

    override fun execute() {
        Log.e("LzXquic", "=========> execute <=========" + Thread.currentThread())
        XquicShortNative().send(
            XquicShortNative.SendParams.Builder()
                .setUrl(host()!!)
                .setToken(null)
                .setSession(null)
                .setContent("我是测试")
                //.setTimeOut(originalRequest.?.)
                //.setMaxRecvLenght(1)
                //.setCCType(XquicShortNative.CCType.RENO)
                .build(), this
        )
    }

    override fun callBackReadData(ret: Int, data: ByteArray) {
        if (ret == 0) {
            val xResponse = XResponse.Builder()
                //.headers()
                .responseBody(XResponseBody(data))
                .request(originalRequest)
                .build()
            responseCallback?.onResponse(xCall, xResponse)
        } else {
            val errMsg = String(data)
            responseCallback?.onFailure(xCall, Exception(errMsg))
        }
    }

    override fun callBackMessage(msgType: Int, data: ByteArray) {
        Log.e("LzXquic", "callBackMessage msgType=$msgType")
    }
}