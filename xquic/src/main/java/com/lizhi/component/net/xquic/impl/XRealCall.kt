package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XRequest

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRealCall : XCall {
    lateinit var xquicClient: XquicClient
    lateinit var originalRequest: XRequest

    // Guarded by this.
    private var executed = false


    companion object {
        fun newCall(xquicClient: XquicClient, xRequest: XRequest): XRealCall {
            val xRealCall = XRealCall()
            xRealCall.xquicClient = xquicClient
            xRealCall.originalRequest = xRequest
            return xRealCall
        }
    }

    override fun enqueue(xCallback: XCallBack?) {
        synchronized(this) {
            check(!executed) { "Already Executed" }
            executed = true
        }
        xquicClient.dispatcher()
            .enqueue(XAsyncCall(this, xquicClient, originalRequest, xCallback))
    }

}