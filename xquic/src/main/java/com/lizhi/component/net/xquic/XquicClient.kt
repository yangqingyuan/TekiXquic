package com.lizhi.component.net.xquic

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lizhi.component.net.xquic.impl.*
import com.lizhi.component.net.xquic.listener.*
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.native.AlpnType
import com.lizhi.component.net.xquic.native.CCType
import com.lizhi.component.net.xquic.native.ProtoVersion
import java.util.*

/**
 * 作用: 短链接
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
open class XquicClient internal constructor(val builder: Builder) {

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
     * proto version
     */
    var protoVersion: Int = builder.protoVersion

    /**
     * dns
     */
    var dns: XDns? = builder.dns

    /**
     * 是否复用
     */
    var reuse: Boolean = builder.reuse

    /**
     * 0RttInfo
     */
    var xRttInfoListener: XRttInfoListener = builder.xRttInfoListener

    /**
     * 协议类型（应用层协议协商（Application-Layer Protocol Negotiation，简称ALPN））
     */
    var alpnType = builder.alpnType


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
    private var xConnectionPool: XConnectionPool? = builder.xConnectionPool

    /**
     * ping listener
     */
    private var pingListener = builder.pingListener

    private var handler = Handler(Looper.getMainLooper())

    /**
     * when lifecycle destroy to cancel tag cell
     */
    private val lifecycleEventObserver = LifecycleEventObserver { source, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            cancel(source.lifecycle.toString())
        }
    }

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
        internal var dispatcher = XDispatcher(XExecutorService().executorService)
        internal var interceptors = mutableListOf<XInterceptor>()
        internal var networkInterceptors = mutableListOf<XInterceptor>()
        internal var xConnectionPool: XConnectionPool? = null
        internal var xRttInfoListener: XRttInfoListener = XRttInfoCache()
        internal var protoVersion: Int = ProtoVersion.XQC_VERSION_V1
        internal var alpnType = AlpnType.ALPN_H3

        internal var pingListener: XPingListener = object : XPingListener {
            override fun ping(): String {
                return "ping"
            }

            override fun pong(data: ByteArray?) {
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
            xRttInfoListener = xquicClient.xRttInfoListener
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

        fun setProtoVersion(@ProtoVersion.Version version: Int) = apply {
            this.protoVersion = version
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

        fun setRttInfo(xRttInfoListener: XRttInfoListener) = apply {
            this.xRttInfoListener = xRttInfoListener
        }

        fun setAlpnType(@AlpnType.Type type: Int) = apply {
            this.alpnType = type
        }

        fun addNetworkInterceptor(xInterceptor: XInterceptor) = apply {
            this.networkInterceptors.add(xInterceptor)
        }
    }

    fun newCall(xRequest: XRequest): XCall {
        /**
         * add lifecycleEventObserver to cancel cell when it is not end
         */
        xRequest.life?.get()?.lifecycle?.let {
            val tag = xRequest.life?.get()?.lifecycle.toString()//use lifecycle to be tag
            xRequest.builder.tag(tag, tag)
            handler.post {
                it.addObserver(lifecycleEventObserver)
            }
        }
        return XRealCall(this, xRequest)
    }

    fun dispatcher(): XDispatcher {
        return dispatcher
    }

    fun connectionPool(): XConnectionPool {
        if (xConnectionPool == null) {//new a connection pool
            xConnectionPool = XConnectionPool()
            builder.xConnectionPool = xConnectionPool
        }
        return xConnectionPool!!
    }

    /**
     * new webSocket
     */
    fun newWebSocket(xRequest: XRequest, listener: XWebSocketListener): XRealWebSocket {
        val xRealWebSocket =
            XRealWebSocket(xRequest, listener, xRttInfoListener, Random(), pingInterval, pingListener)
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