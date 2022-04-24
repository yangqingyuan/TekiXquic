package com.lizhi.component.net.xquic.impl

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
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
    lateinit var asyncCall: XAsyncCall

    // Guarded by this.
    private var executed = false

    companion object {

        private val handle = Handler(Looper.getMainLooper())

        fun newCall(xquicClient: XquicClient, xRequest: XRequest): XRealCall {
            val xRealCall = XRealCall()
            xRealCall.xquicClient = xquicClient
            xRealCall.originalRequest = xRequest

            /* auto cancel by activity left */
            if (xRealCall.originalRequest.life is LifecycleOwner) {
                handle.post {
                    xRealCall.originalRequest.life?.lifecycle?.addObserver(object :
                        LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            if (event == Lifecycle.Event.ON_DESTROY) {
                                xRealCall.cancel()
                            }
                        }
                    })
                }
            }

            return xRealCall
        }
    }

    override fun request(): XRequest {
        return originalRequest
    }

    override fun enqueue(xCallback: XCallBack?) {
        synchronized(this) {
            check(!executed) { "Already Executed" }
            executed = true
        }
        asyncCall = XAsyncCall(this, xquicClient, originalRequest, xCallback)
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