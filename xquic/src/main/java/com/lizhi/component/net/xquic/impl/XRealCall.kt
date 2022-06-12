package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.listener.XRunnable
import com.lizhi.component.net.xquic.mode.XRequest

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRealCall(private val xquicClient: XquicClient, private val originalRequest: XRequest) :
    XCall {

    // Guarded by this.
    private var executed = false

    private lateinit var asyncCall: XRunnable

    override fun request(): XRequest {
        return originalRequest
    }

    override fun enqueue(xCallback: XCallBack?) {
        synchronized(this) {
            check(!executed) { "Already Executed" }
            executed = true
        }
        asyncCall = if (xquicClient.reuse) {//if reuse
            XAsyncCallReuse(
                this,
                xquicClient,
                originalRequest,
                xCallback
            )
        } else {
            XAsyncCall(this, xquicClient, originalRequest, xCallback)
        }
        xquicClient.dispatcher()
            .enqueue(asyncCall)
    }


    fun get(): XRealCall {
        return this
    }

    override fun cancel() {
        asyncCall.cancel()
    }

    override fun isExecuted(): Boolean {
        return executed
    }

    override fun isCanceled(): Boolean {
        return true
    }
}