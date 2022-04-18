package com.lizhi.component.net.xquic

import com.lizhi.component.net.xquic.impl.XDispatcher
import com.lizhi.component.net.xquic.impl.XRealCall
import com.lizhi.component.net.xquic.impl.XRealWebSocket
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XInterceptor
import com.lizhi.component.net.xquic.listener.XPingListener
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.native.CCType
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.util.*

/**
 * 作用: 短链接
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XquicClient {

    companion object {
        /**
         * 默认的ping实现
         */
        private var DEFAULT_PING_LISTENER: XPingListener = object : XPingListener {
            override fun ping(): String {
                return "ping"
            }

            override fun pong(data: String) {
                XLogUtils.debug("pong callBack= $data")
            }
        }
    }

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
     * unit second
     */
    var pingInterval: Long = 0L

    /**
     * 拥塞算法
     */
    var ccType = CCType.CUBIC

    private val dispatcher by lazy { XDispatcher() }

    /**
     * 拦截器
     */
    private val interceptors by lazy { mutableListOf<XInterceptor>() }

    /**
     * 网络拦截器
     */
    private val networkInterceptors by lazy { mutableListOf<XInterceptor>() }

    /**
     * ping listener
     */
    private var pingListener = DEFAULT_PING_LISTENER


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

        fun pingInterval(pingInterval: Long): Builder {
            xquicClient.pingInterval = pingInterval
            return this
        }

        fun addInterceptor(xInterceptor: XInterceptor): Builder {
            xquicClient.interceptors.add(xInterceptor)
            return this
        }

        fun addPingListener(pingListener: XPingListener) {
            xquicClient.pingListener = pingListener
        }

        fun addNetworkInterceptor(xInterceptor: XInterceptor): Builder {
            xquicClient.networkInterceptors.add(xInterceptor)
            return this
        }
    }

    fun newCall(xRequest: XRequest): XCall {
        return XRealCall.newCall(this, xRequest)
    }

    fun dispatcher(): XDispatcher {
        return dispatcher
    }


    /**
     * new webSocket
     */
    fun newWebSocket(xRequest: XRequest, listener: XWebSocketListener): XRealWebSocket {
        val xRealWebSocket =
            XRealWebSocket(xRequest, listener, Random(), pingInterval, pingListener)
        xRealWebSocket.connect(this)
        return xRealWebSocket
    }


}