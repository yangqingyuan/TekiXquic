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
open class XquicClient internal constructor(builder: Builder) {

    /**
     * unit second
     */
    var connectTimeOut: Int = builder.connectTimeOut

    /**
     * unit second
     */
    var readTimeout: Int = builder.readTimeout

    /**
     * unit second TODO 待实现
     */
    var writeTimeout: Int = builder.writeTimeout

    /**
     * unit MILLISECONDS
     */
    var pingInterval: Long = builder.pingInterval

    /**
     * 拥塞算法
     */
    var ccType = builder.ccType

    /**
     * dns
     */
    var dns: XDns? = builder.dns

    /**
     * 是否复用
     */
    var reuse: Boolean = builder.reuse


    private val dispatcher = builder.dispatcher

    /**
     * 拦截器
     */
    private val interceptors = builder.interceptors

    /**
     * 网络拦截器
     */
    private val networkInterceptors = builder.networkInterceptors

    /**
     * 链接管理
     */
    private var xConnectionPool = builder.xConnectionPool

    /**
     * ping listener
     */
    private var pingListener = builder.pingListener

    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder constructor() {
        internal var connectTimeOut: Int = 30
        internal var readTimeout: Int = 30
        internal var writeTimeout: Int = 0
        internal var pingInterval: Long = 0L
        internal var ccType = CCType.CUBIC
        internal var dns: XDns? = null
        internal var reuse: Boolean = false
        internal var dispatcher = XDispatcher()
        internal var interceptors = mutableListOf<XInterceptor>()
        internal var networkInterceptors = mutableListOf<XInterceptor>()
        internal var xConnectionPool: XConnectionPool = XConnectionPool()
        internal var pingListener: XPingListener = object : XPingListener {
            override fun ping(): String {
                return "ping"
            }

            override fun pong(data: String) {
                //XLogUtils.debug("pong callBack= $data")
            }
        }

        internal constructor(xquicClient: XquicClient) : this() {
            connectTimeOut = xquicClient.connectTimeOut
            readTimeout = xquicClient.readTimeout
            writeTimeout = xquicClient.writeTimeout
            pingInterval = xquicClient.pingInterval
            ccType = xquicClient.ccType
            dns = xquicClient.dns
            reuse = xquicClient.reuse
            dispatcher = xquicClient.dispatcher
            interceptors = xquicClient.interceptors
            networkInterceptors = xquicClient.networkInterceptors
            xConnectionPool = xquicClient.xConnectionPool
            pingListener = xquicClient.pingListener
        }

        fun build(): XquicClient {
            return XquicClient(this)
        }

        fun connectTimeOut(connectTimeout: Int) = apply {
            this.connectTimeOut = connectTimeout
        }

        fun setReadTimeOut(readTimeout: Int) = apply {
            this.readTimeout = readTimeout
        }

        fun writeTimeout(writeTimeout: Int) = apply {
            this.writeTimeout = writeTimeout
        }

        fun ccType(@CCType.Type ccType: Int) = apply {
            this.ccType = ccType
        }

        fun pingInterval(pingInterval: Long) = apply {
            this.pingInterval = pingInterval
        }

        fun dns(xDns: XDns) = apply {
            this.dns = xDns
        }

        fun addInterceptor(xInterceptor: XInterceptor) = apply {
            this.interceptors.add(xInterceptor)
        }

        fun addPingListener(pingListener: XPingListener) = apply {
            this.pingListener = pingListener
        }

        fun reuse(isReuse: Boolean) = apply {
            this.reuse = isReuse
        }

        fun connectionPool(xConnectionPool: XConnectionPool) = apply {
            this.xConnectionPool = xConnectionPool
        }

        fun addNetworkInterceptor(xInterceptor: XInterceptor) = apply {
            this.networkInterceptors.add(xInterceptor)
        }
    }

    fun newCall(xRequest: XRequest): XCall {
        return XRealCall(this, xRequest)
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