package com.lizhi.component.net.xquic

import com.lizhi.component.net.xquic.impl.XDispatcher
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.impl.XRealCall
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.native.CCType

/**
 * 作用: 短链接
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XquicClient {

    /**
     * unit second
     */
    var connectTimeOut: Int = 30

    /**
     * unit second TODO 待实现
     */
    var readTimeout: Int = 0

    /**
     * unit second TODO 待实现
     */
    var writeTimeout: Int = 0

    /**
     * unit second TODO 待实现
     */
    var pingInterval: Int = 0

    /**
     * 拥塞算法
     */
    var ccType = CCType.CUBIC

    private val dispatcher by lazy { XDispatcher() }

    class Builder {
        private val xquicClient = XquicClient()

        fun build(): XquicClient {
            return xquicClient
        }

        fun connectTimeOut(connectTimeout: Int): Builder {
            xquicClient.connectTimeOut = connectTimeout
            return this
        }

        fun setReadTimeOut(readTimeout: Int): Builder {
            xquicClient.readTimeout = readTimeout
            return this
        }

        fun writeTimeout(writeTimeout: Int): Builder {
            xquicClient.writeTimeout = writeTimeout
            return this
        }

        fun ccType(ccType: CCType): Builder {
            xquicClient.ccType = ccType
            return this
        }

        fun pingInterval(pingInterval: Int): Builder {
            xquicClient.pingInterval = pingInterval
            return this
        }
    }

    fun newCall(xRequest: XRequest): XCall {
        return XRealCall.newCall(this, xRequest)
    }

    fun dispatcher(): XDispatcher {
        return dispatcher
    }


}