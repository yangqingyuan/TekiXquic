package com.lizhi.component.net.xquic

import com.lizhi.component.net.xquic.impl.XConnectionPool
import com.lizhi.component.net.xquic.impl.XDispatcher
import com.lizhi.component.net.xquic.impl.XRealCall
import com.lizhi.component.net.xquic.impl.XRealWebSocket
import com.lizhi.component.net.xquic.listener.*
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.native.CCType
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
                //XLogUtils.debug("pong callBack= $data")
            }
        }
    }

    /**
     * unit second
     */
    var connectTimeOut: Int = 30

    /**
     * unit second
     */
    var readTimeout: Int = 30

    /**
     * unit second TODO 待实现
     */
    var writeTimeout: Int = 0

    /**
     * unit MILLISECONDS
     */
    var pingInterval: Long = 0L

    /**
     * 拥塞算法
     */
    var ccType = CCType.CUBIC

    /**
     * dns
     */
    var dns: XDns? = null

    /**
     * 是否复用
     */
    var reuse: Boolean = false


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
     * 链接管理
     */
    private var xConnectionPool: XConnectionPool = XConnectionPool()

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

        fun ccType(@CCType.Type ccType: Int): Builder {
            xquicClient.ccType = ccType
            return this
        }

        fun pingInterval(pingInterval: Long): Builder {
            xquicClient.pingInterval = pingInterval
            return this
        }

        fun dns(xDns: XDns): Builder {
            xquicClient.dns = xDns
            return this
        }

        fun addInterceptor(xInterceptor: XInterceptor): Builder {
            xquicClient.interceptors.add(xInterceptor)
            return this
        }

        fun addPingListener(pingListener: XPingListener): Builder {
            xquicClient.pingListener = pingListener
            return this
        }

        fun reuse(isReuse: Boolean): Builder {
            xquicClient.reuse = isReuse
            return this
        }

        fun connectionPool(xConnectionPool: XConnectionPool) {
            xquicClient.xConnectionPool = xConnectionPool
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

    fun connectionPool(): XConnectionPool {
        return xConnectionPool
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

    /**
     * cancel by tag
     */
    fun cancel(tag: String) {
        dispatcher.cancel(tag)
    }

}