package com.lizhi.component.net.xquic.impl

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XHeaders
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.mode.XResponseBody
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicMsgType
import com.lizhi.component.net.xquic.native.XquicShortNative
import com.lizhi.component.net.xquic.utils.XLogUtils
import org.json.JSONObject
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

    /**
     * xResponse
     */
    private var xResponse: XResponse

    /**
     * is finish
     */
    private var isFinish = false

    /**
     * short native
     */
    private val xquicShortNative = XquicShortNative()

    private var clientCtx: Long = 0L

    private var executed = false

    private val handle:Handler = object :Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            responseCallback?.onFailure(xCall, Exception("time out"))
            cancel()
        }
    }


    init {
        index = atomicInteger.incrementAndGet()
        name = String.format(Locale.US, "${XLogUtils.commonTag} %s", originalRequest.url)

        xResponse = XResponse.Builder()
            .headers(originalRequest.headers.build())
            .request(originalRequest)
            .delayTime(delayTime)
            .index(index)
            .build()

        /*
         * set timeout
         */
        if (xquicClient.readTimeout > 0) {
            handle.sendEmptyMessageDelayed(index, xquicClient.readTimeout * 1000L)
        }
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
        return originalRequest.url.getNewUrl()
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

    override fun execute() {
        val startTime = System.currentTimeMillis()
        delayTime = startTime - createTime
        executed = true
        try {
            XLogUtils.debug("=======> execute start index(${index})<========")
            XLogUtils.debug(" url ${url()} ")

            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url())
                .setToken(XRttInfoCache.tokenMap[originalRequest.url.host])
                .setSession(XRttInfoCache.sessionMap[originalRequest.url.host])
                .setConnectTimeOut(xquicClient.connectTimeOut)
                .setReadTimeOut(xquicClient.readTimeout)
                .setMaxRecvLenght(1024 * 1024)
                .setCCType(xquicClient.ccType)

            sendParamsBuilder.setHeaders(parseHttpHeads())

            originalRequest.body?.let {
                sendParamsBuilder.setContent(it.content)
            }

            /* native to send */
            xquicShortNative.send(
                sendParamsBuilder.build(), this
            )
            clientCtx = 0L
            handle.removeMessages(index)
        } catch (e: Exception) {
            cancel()
        } finally {
            XLogUtils.debug("=======> execute end cost(${System.currentTimeMillis() - startTime} ms),index(${index})<========")
            isFinish = true
            xquicClient.dispatcher().finished(this)
        }

    }

    override fun callBackData(ret: Int, data: ByteArray) {

        synchronized(isCallback) {
            if (isCallback) {
                XLogUtils.error(
                    "is callback on need to callback again!! ret=${ret},data=${
                        String(
                            data
                        )
                    }"
                )
                return@synchronized
            }
            if (ret == XquicCallback.XQC_OK) {
                xResponse.xResponseBody = XResponseBody(data)
                xResponse.code = ret
                responseCallback?.onResponse(xCall, xResponse)
            } else {
                val errMsg = String(data)
                responseCallback?.onFailure(xCall, Exception(errMsg))
            }
            isCallback = true
        }
    }

    override fun callBackMessage(msgType: Int, data: ByteArray) {
        XLogUtils.debug("callBackMessage msgType=$msgType")

        synchronized(this) {
            when (msgType) {
                XquicMsgType.INIT.ordinal -> {
                    try {
                        clientCtx = String(data).toLong()
                    } catch (e: Exception) {
                        XLogUtils.error(e)
                    }
                }

                XquicMsgType.TOKEN.ordinal -> {
                    XRttInfoCache.tokenMap.put(originalRequest.url.host, String(data))
                }
                XquicMsgType.SESSION.ordinal -> {
                    XRttInfoCache.sessionMap.put(originalRequest.url.host, String(data))
                }

                XquicMsgType.DESTROY.ordinal -> {
                    clientCtx = 0L
                }

                XquicMsgType.TP.ordinal -> {
                    XRttInfoCache.tpMap.put(url(), String(data))
                }
                XquicMsgType.HEAD.ordinal -> {
                    try {
                        val headJson = JSONObject(String(data))
                        val xHeaderBuild = XHeaders.Builder()
                        for (key in headJson.keys()) {
                            xHeaderBuild.add(key, headJson.getString(key))
                        }
                        xResponse.xHeaders = xHeaderBuild.build()
                    } catch (e: Exception) {
                        XLogUtils.error(e)
                    }
                }
                else -> {
                    //XLogUtils.error("un know callback msg")
                }
            }
        }
    }

    fun get(): XCall {
        return xCall
    }

    fun cancel() {
        handle.removeMessages(index)
        if (!isFinish && clientCtx > 0) {
            xquicShortNative.cancel(clientCtx)
        }

        if (!executed) {
            xquicClient.dispatcher().finished(this)
        }
    }

}