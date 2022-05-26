package com.lizhi.component.net.xquic.impl

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.listener.XRunnable
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.native.XquicShortNative
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.io.InterruptedIOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger

/**
 * 作用: 异步调用公共逻辑
 * 作者: yqy
 * 创建日期: 2022/5/26.
 */
abstract class XAsyncCallCommon(
    private var xCall: XCall,
    private var xquicClient: XquicClient,
    private var originalRequest: XRequest,
    private var responseCallback: XCallBack? = null
) : Runnable, XRunnable {
    var name: String? = null

    override fun run() {
        val oldName = Thread.currentThread().name
        Thread.currentThread().name = name
        try {
            execute()
        } finally {
            Thread.currentThread().name = oldName
        }
    }


    companion object {
        /**
         * create index
         */
        private val atomicInteger = AtomicInteger()
    }

    var index = 0


    var createTime = System.currentTimeMillis()

    /**
     * queue delay time
     */
    var delayTime = 0L

    /**
     * isCallback
     */
    var isCallback = false

    /**
     * xResponse
     */
    var xResponse: XResponse

    /**
     * is finish
     */
    var isFinish = false

    /**
     * short native
     */
    val xquicShortNative = XquicShortNative()

    var clientCtx: Long = 0L

    var executed = false

    init {
        index = atomicInteger.incrementAndGet()
        name = String.format(Locale.US, "${XLogUtils.commonTag} %s", originalRequest.url)

        xResponse = XResponse.Builder()
            .headers(originalRequest.headers.build())
            .request(originalRequest)
            .delayTime(delayTime)
            .index(index)
            .build()

    }

    val handle: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            responseCallback?.onFailure(xCall, Exception("read time out"))
            cancel()
        }
    }


    override fun executeOn(executorService: ExecutorService?) {
        assert(!Thread.holdsLock(xquicClient.dispatcher()))

        // set timeout
        if (xquicClient.readTimeout > 0) {
            handle.sendEmptyMessageDelayed(index, xquicClient.readTimeout * 1000L)
        }
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

    /**
     * parse http heads
     * more headers "https://zhuanlan.zhihu.com/p/282737965"
     */
    fun parseHttpHeads(): HashMap<String, String> {
        /* set headers */
        val headers = hashMapOf<String, String>()
        headers[":method"] = originalRequest.method
        headers[":scheme"] = originalRequest.url.scheme
        headers[":authority"] = originalRequest.url.authority
        originalRequest.url.path?.let {
            headers[":path"] = it
        }

        headers.putAll(originalRequest.headers.build().headersMap)
        originalRequest.body?.let {
            val body = it
            headers["content-type"] = body.mediaType.mediaType
            headers["content-length"] = body.content.length.toString()
        }
        return headers
    }


    fun authority(): String {
        return originalRequest.url.authority
    }

    override fun url(): String {
        return originalRequest.url.url
    }

    override fun get(): XCall {
        return xCall
    }

    override fun cancel() {
        handle.removeMessages(index)
    }
}