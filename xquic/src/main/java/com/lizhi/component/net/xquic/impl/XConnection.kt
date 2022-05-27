package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicLongNative
import com.lizhi.component.net.xquic.native.XquicMsgType
import com.lizhi.component.net.xquic.utils.XLogUtils

/**
 * 作用: 链接
 * 作者: yqy
 * 创建日期: 2022/5/26.
 */
class XConnection(val xRequest: XRequest, private val xDispatcher: XDispatcher) : XquicCallback {

    companion object {
        private const val TAG = "XConnection"

    }

    private var clientCtx: Long = 0L

    /**
     * long native
     */
    private val xquicLongNative = XquicLongNative()

    private var xCallBackList: MutableList<XquicCallback> = mutableListOf()

    private var isLive = false

    var idleAtNanos = Long.MAX_VALUE


    fun send(params: SendParams, xCallBack: XquicCallback) {
        synchronized(this) {
            xCallBackList.add(xCallBack)
            params.content = if (params.content.isNullOrEmpty()) {
                Message("test").getContent()
            } else {
                Message(params.content!!).getContent()
            }
            if (isLive && clientCtx > 0) {
                xquicLongNative.sendWithHead(clientCtx, params, params.content!!)
            } else {
                startConnect(params)
            }
        }
    }

    private fun startConnect(params: SendParams) {
        xDispatcher.executorService()?.execute {
            try {
                clientCtx = xquicLongNative.connect(params, this)
                if (clientCtx <= 0) {
                    throw java.lang.Exception("connect error ,clientCtx =$clientCtx")
                } else {
                    /* 注意：这里是阻塞的 */
                    isLive = true
                    xquicLongNative.start(clientCtx)
                    clientCtx = 0
                }
            } catch (e: Exception) {
                XLogUtils.error(e)
            } finally {
                XLogUtils.info("=================connection is end==============")
            }
        }
    }

    /**
     * message object
     */
    internal class Message(var msgContent: String, var tag: String? = null) {
        fun getContent(): String {
            if (tag != null && tag!!.length > 512) {
                return "{\"send_body\":\"$msgContent\",\"user_tag\":\"${"tag is too lang"}\"}"
            }
            return "{\"send_body\":\"$msgContent\",\"user_tag\":\"${tag ?: ""}\"}"
        }
    }

    private fun authority(): String {
        return xRequest.url.authority
    }

    override fun callBackData(ret: Int, data: String) {
        idleAtNanos = System.nanoTime()
        xCallBackList.forEach {
            it.callBackData(ret, data)
        }
        xCallBackList.clear()
    }

    override fun callBackMessage(msgType: Int, data: String) {
        xCallBackList.forEach {
            it.callBackMessage(msgType, data)
        }

        synchronized(this) {
            when (msgType) {
                XquicMsgType.HANDSHAKE.ordinal -> {

                }

                XquicMsgType.TOKEN.ordinal -> {
                    XRttInfoCache.tokenMap.put(authority(), data)
                }

                XquicMsgType.SESSION.ordinal -> {
                    XRttInfoCache.sessionMap.put(authority(), data)
                }

                XquicMsgType.TP.ordinal -> {
                    XRttInfoCache.tpMap.put(authority(), data)
                }

                XquicMsgType.TP.ordinal -> {

                }
                XquicMsgType.HEAD.ordinal -> {

                }
                XquicMsgType.PING.ordinal -> {

                }
                XquicMsgType.DESTROY.ordinal -> {
                    isLive = false
                }
                else -> {
                    XLogUtils.error("un know callback msg")
                }
            }
        }
    }

    /**
     * 是否符合条件
     */
    fun isEligible(xRequest: XRequest): Boolean {
        if (!isLive || clientCtx < 0) {
            return false
        }
        if (this.xRequest.url.authority == xRequest.url.authority) {//通过域名来判断是否符合
            return true
        }
        return false
    }

    fun close() {
        isLive = false
        if (clientCtx > 0) {
            xquicLongNative.cancel(clientCtx)
            clientCtx = 0
        }
    }
}