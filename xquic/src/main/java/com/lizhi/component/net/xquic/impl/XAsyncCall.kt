package com.lizhi.component.net.xquic.impl

import android.util.LruCache
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.mode.XResponseBody
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicMsgType
import com.lizhi.component.net.xquic.native.XquicShortNative
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.io.InterruptedIOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap


/**
 * 作用: 异步调用
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XAsyncCall(
    private var xCall: XCall,
    private var xquicClient: XquicClient,
    private var originalRequest: XRequest,
    private var responseCallback: XCallBack? = null
) : XNamedRunnable(), XquicCallback {

    companion object {

        /**
         * token
         */
        private val tokenMap by lazy { LruCache<String, String>(100) }

        /**
         * session
         */
        private val sessionMap by lazy { LruCache<String, String>(100) }

        /**
         * tp
         */
        private val tpMap by lazy { LruCache<String, String>(100) }

        /**
         * create index
         */
        private val atomicInteger = AtomicInteger()
    }

    private var index = 0
    private var createTime = System.currentTimeMillis()

    /**
     * queue delay time
     */
    private var delayTime = 0L

    /**
     * isCallback
     */
    private var isCallback = false


    init {
        index = atomicInteger.incrementAndGet()
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
        return originalRequest.url.url
    }

    fun request(): XRequest {
        return originalRequest
    }

    /**
     * parse http heads
     * more headers "https://zhuanlan.zhihu.com/p/282737965"
     */
    private fun parseHttpHeads(): HashMap<String, String> {
        /* set headers */
        val headers = hashMapOf<String, String>()
        headers[":method"] = originalRequest.method
        headers[":scheme"] = originalRequest.url.scheme
        originalRequest.url.path?.let {
            headers[":path"] = it
        }
        originalRequest.url.authority?.let {
            headers[":authority"] = it
        }

        headers.putAll(originalRequest.headers.build().headersMap)
        if (originalRequest.method == "POST") {
            val body = originalRequest.body
            headers["content-type"] = body.mediaType.mediaType
            headers["content-length"] = body.content.length.toString()
        }
        return headers
    }

    override fun execute() {
        val startTime = System.currentTimeMillis()
        delayTime = startTime - createTime
        try {
            XLogUtils.debug("=======> execute start index(${index})<========")
            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url())
                .setToken(tokenMap[url()])
                .setSession(sessionMap[url()])
                .setTimeOut(xquicClient.connectTimeOut)
                .setMaxRecvLenght(1024 * 1024)
                .setCCType(xquicClient.ccType)

            sendParamsBuilder.setHeaders(parseHttpHeads())

            if (originalRequest.method == "POST") {
                sendParamsBuilder.setContent(originalRequest.body.content)
            }

            /* native to send */
            XquicShortNative().send(
                sendParamsBuilder.build(), this
            )
        } catch (e: Exception) {
            cancel()
        } finally {
            XLogUtils.debug("=======> execute end cost(${System.currentTimeMillis() - startTime} ms),index(${index})<========")
            xquicClient.dispatcher().finished(this)
        }

    }

    override fun callBackReadData(ret: Int, data: ByteArray) {
        synchronized(isCallback) {
            if (isCallback) {
                XLogUtils.error(
                    "is callback on need to callback again!! ret=${ret},data=${
                        String(
                            byteArrayOf()
                        )
                    }"
                )
                return@synchronized
            }
            isCallback = true
            if (ret == 0) {
                val xResponse = XResponse.Builder()
                    .headers(originalRequest.headers.build())
                    .responseBody(XResponseBody(data))
                    .request(originalRequest)
                    .delayTime(delayTime)
                    .index(index)
                    .build()
                xResponse.code = ret
                responseCallback?.onResponse(xCall, xResponse)
            } else {
                val errMsg = String(data)
                responseCallback?.onFailure(xCall, Exception(errMsg))
            }
        }
    }

    override fun callBackMessage(msgType: Int, data: ByteArray) {
        XLogUtils.debug("callBackMessage msgType=$msgType")

        synchronized(this) {
            when (msgType) {
                XquicMsgType.TOKEN.ordinal -> {
                    tokenMap.put(url(), String(data))
                }
                XquicMsgType.SESSION.ordinal -> {
                    sessionMap.put(url(), String(data))
                }
                XquicMsgType.TP.ordinal -> {
                    tpMap.put(url(), String(data))
                }
                else -> {
                    XLogUtils.error("un know callback msg")
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