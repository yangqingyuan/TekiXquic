package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicShortNative
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception


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
) : XAsyncCallCommon(xCall, xquicClient, originalRequest, responseCallback), XquicCallback {
    /**
     * short native
     */
    val xquicShortNative = XquicShortNative()

    override fun execute() {
        val startTime = System.currentTimeMillis()
        delayTime = startTime - createTime
        executed = true
        try {
            XLogUtils.debug("=======> execute start indexAA(${index})<========")
            val url = originalRequest.url.getHostUrl(xquicClient.dns)
            if (url.isNullOrBlank()) {
                responseCallback?.onFailure(
                    xCall,
                    Exception("dns can not parse domain ${originalRequest.url.url} error")
                )
                return
            }
            XLogUtils.debug(" url $url ")

            val sendParamsBuilder = SendParams.Builder()
                .setUrl(url)
                .setToken(XRttInfoCache.tokenMap[authority()])
                .setSession(XRttInfoCache.sessionMap[authority()])
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
            e.printStackTrace()
            XLogUtils.error(e)
            cancel()
        } finally {
            XLogUtils.debug("=======> execute end cost(${System.currentTimeMillis() - startTime} ms),index(${index})<========")
            isFinish = true
            xquicClient.dispatcher().finished(this)
        }

    }

    override fun cancel() {
        super.cancel()
        if (!isFinish && clientCtx > 0) {
            xquicShortNative.cancel(clientCtx)
        }
    }

}