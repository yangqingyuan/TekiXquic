package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XHeaders
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponseBody
import com.lizhi.component.net.xquic.native.*
import com.lizhi.component.net.xquic.utils.XLogUtils
import org.json.JSONObject


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

    @Volatile
    var clientCtx: Long = 0L

    /**
     * short native
     */
    private val xquicShortNative = XquicShortNative()

    /**
     * xRttInfo
     */
    private val xRttInfoCache = xquicClient.xRttInfoListener


    override fun execute() {
        val startTime = System.currentTimeMillis()
        delayTime = startTime - createTime
        try {
            XLogUtils.debug("=======> execute start indexAA(${indexTag})<========")
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
                .setToken(xRttInfoCache.getToken(authority()))
                .setSession(xRttInfoCache.getSession(authority()))
                .setConnectTimeOut(xquicClient.connectTimeOut)
                .setReadTimeOut(xquicClient.readTimeout)
                .setMaxRecvLenght(1024 * 1024)
                .setCCType(xquicClient.ccType)
                .setCryptoFlag(xquicClient.cryptoFlag)
                .setProtoVersion(xquicClient.protoVersion)
                .setFinishFlag(xquicClient.finishFlag)
                .setAlpnType(xquicClient.alpnType)

            sendParamsBuilder.setHeaders(parseHttpHeads())

            originalRequest.body?.let {
                sendParamsBuilder.setContent(it.content)
            }

            /* native to send */
            xquicShortNative.send(
                sendParamsBuilder.build(), this
            )
            clientCtx = 0L
            handle.removeMessages(indexTag)
        } catch (e: Exception) {
            e.printStackTrace()
            XLogUtils.error(e)
            cancel()
        } finally {
            XLogUtils.debug("=======> execute end cost(${System.currentTimeMillis() - startTime} ms),index(${indexTag})<========")
            isFinish = true
            xquicClient.dispatcher().finished(this)
        }

    }

    override fun cancel() {
        try {
            super.cancel()
            if (!isFinish && checkClientCtx(clientCtx)) {
                xquicShortNative.cancel(clientCtx)
            }
        } catch (e: Exception) {
            XLogUtils.error(e)
        }
    }

    override fun callBackMessage(msgType: Int, data: ByteArray) {
        synchronized(this) {
            when (msgType) {
                XquicMsgType.INIT -> {
                    try {
                        clientCtx = String(data).toLong()
                    } catch (e: Exception) {
                        XLogUtils.error(e)
                    }
                }

                XquicMsgType.TOKEN -> {
                    xRttInfoCache.tokenBack(authority(), data)
                }
                XquicMsgType.SESSION -> {
                    xRttInfoCache.sessionBack(authority(), data)
                }

                XquicMsgType.DESTROY -> {
                    clientCtx = 0L
                }

                XquicMsgType.TP -> {

                }
                XquicMsgType.HEAD -> {
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


    override fun callBackData(ret: Int, tag: String?, data: ByteArray) {
        synchronized(isCallback) {
            if (isCallback) {
                XLogUtils.warn(
                    "is callback on need to callback again!! ret=${ret},data=${data}"
                )
                return@synchronized
            }
            handle.removeMessages(indexTag)
            if (ret == XquicCallback.XQC_OK) {
                xResponse.xResponseBody = XResponseBody(data, userTag)
                xResponse.code = ret
                responseCallback?.onResponse(xCall, xResponse)
            } else {
                responseCallback?.onFailure(
                    xCall,
                    Exception(String(data))
                )
                cancel()// cancel now
            }
            isCallback = true
        }
    }
}