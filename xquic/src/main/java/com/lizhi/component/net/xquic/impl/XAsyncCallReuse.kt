package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception
import kotlin.properties.Delegates

/**
 * 作用: 复用调用
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XAsyncCallReuse(
    private var xCall: XCall,
    private var xquicClient: XquicClient,
    private var xRequest: XRequest,
    private var responseCallback: XCallBack? = null,
) : XAsyncCallCommon(xCall, xquicClient, xRequest, responseCallback) {

    companion object {
        const val TAG = "XAsyncCallReuse"
    }

    var connection: XConnection? = null
    private var startTime by Delegates.notNull<Long>()

    override fun execute() {
        startTime = System.currentTimeMillis()
        try {
            XLogUtils.debug("=======> execute start index(${indexTag})<========")
            val url = xRequest.url.getHostUrl(xquicClient.dns)
            if (url.isNullOrBlank()) {
                responseCallback?.onFailure(
                    xCall,
                    Exception("dns can not parse domain ${xRequest.url.url} error")
                )
                return
            }
            XLogUtils.debug(" url $url ")

            synchronized(TAG) {
                connection = xquicClient.connectionPool().get(xRequest)
                if (connection == null || connection?.isDestroy == true) {//create new connection
                    connection = XConnection(xquicClient, xRequest)
                    xquicClient.connectionPool().put(connection!!) //add to pool
                }
            }

            //注意：这里使用index作为tag
            connection!!.send(
                indexTag.toString(),
                xRequest.body?.content, parseHttpHeads(),
                object : XCallBack {
                    override fun onFailure(call: XCall, exception: Exception) {
                        responseCallback?.onFailure(xCall, exception)
                        finish(false)
                    }

                    override fun onResponse(call: XCall, xResponse: XResponse) {
                        xResponse.xResponseBody.tag = userTag
                        responseCallback?.onResponse(xCall, xResponse)
                        finish(true)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            XLogUtils.error(e)
            cancel()
            finish(false)
        }
    }

    fun finish(result: Boolean) {
        XLogUtils.debug(
            "=======> execute status(${
                if (result) {
                    "success"
                } else {
                    "failed"
                }
            }),cost(${System.currentTimeMillis() - startTime} ms),index(${indexTag})<========"
        )
        handle.removeMessages(indexTag)
        isFinish = true
        xquicClient.dispatcher().finished(this@XAsyncCallReuse)
    }

    override fun cancel() {
        try {
            super.cancel()
            connection?.cancel(indexTag.toString())
        } catch (e: Exception) {
            XLogUtils.error(e)
        }
    }
}