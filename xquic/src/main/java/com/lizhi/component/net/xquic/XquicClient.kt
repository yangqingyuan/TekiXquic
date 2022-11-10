package com.lizhi.component.net.xquic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lizhi.component.net.xquic.impl.*
import com.lizhi.component.net.xquic.listener.*
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.quic.*
import com.lizhi.component.net.xquic.quic.CCType
import com.lizhi.component.net.xquic.utils.XLogUtils
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

    /**
     * 0: crypto
     * 1: 1:without crypto
     */
    var cryptoFlag = builder.cryptoFlag

    /**
     * request finish flag, 1 for finish.
     */
    var finishFlag = builder.finishFlag

    /**
     * The buffer that accepts data after each request. The default is 512kb.
     * If you already know the return size of back-end data,
     * you can set the corresponding settings to facilitate optimal memory usage
     */
    var maxRecvDataLen = builder.maxRecvDataLen

    /**
     * If the timeout setting of ping return times indicates that the callback will fail
     */
    internal var pingTimeOutCount: Int = 0

    /**
     * 调度器
     */
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

    private var context: Context? = builder.context

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
        internal var cryptoFlag = CryptoFlag.CRYPTO
        internal var finishFlag = FinishFlag.FINISH
        internal var maxRecvDataLen = 1024 * 512
        internal var pingTimeOutCount: Int = 0
        internal var context: Context? = null

        internal var pingListener: XPingListener = object : XPingListener {
            override fun ping(): ByteArray {
                return "ping".toByteArray()
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
            cryptoFlag = xquicClient.cryptoFlag
            cryptoFlag = xquicClient.finishFlag
            maxRecvDataLen = xquicClient.maxRecvDataLen
            context = xquicClient.context
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

        /**
         * if pingInterval>0 send ping
         */
        fun pingInterval(pingInterval: Long, pingTimeOutCount: Int = 0) = apply {
            this.pingInterval = pingInterval
            this.pingTimeOutCount = pingTimeOutCount
        }

        fun setCryptoFlag(@CryptoFlag.Type flag: Int) = apply {
            this.cryptoFlag = flag
        }

        fun setFinishFlag(@FinishFlag.Type flag: Int) = apply {
            this.finishFlag = flag
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

        fun setMaxRecvDataLen(MaxRecvDataLen: Int) = apply {
            this.maxRecvDataLen = MaxRecvDataLen
        }

        fun addNetworkInterceptor(xInterceptor: XInterceptor) = apply {
            this.networkInterceptors.add(xInterceptor)
        }

        fun setContext(context: Context) = apply {
            this.context = context.applicationContext
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
     * 建议不直接使用该函数，使用XquicClient短链接，并将复用打开，内部也是使用了XRealWebSocket来实现
     */
    fun newWebSocket(xRequest: XRequest, listener: XWebSocketListener): XRealWebSocket {
        val xRealWebSocket =
            XRealWebSocket(
                xRequest,
                listener,
                xRttInfoListener,
                Random(),
                pingInterval,
                pingListener,
                pingTimeOutCount
            )
        xRealWebSocket.connect(this)
        this.context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                registerNetwork(it)
            }
        }
        return xRealWebSocket
    }

    /**
     * register network change callback
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun registerNetwork(context: Context) {
        XLogUtils.debug("registerNetwork")
        synchronized(XNetStatusCallBack.isRegister) {
            if (!XNetStatusCallBack.isRegister) {
                XNetStatusCallBack.isRegister = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val connMgr =
                        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    connMgr.registerDefaultNetworkCallback(XNetStatusCallBack.xNetStatusManager)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val request = NetworkRequest.Builder()
                        .build()
                    val connMgr =
                        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    connMgr.registerNetworkCallback(request, XNetStatusCallBack.xNetStatusManager)
                }
            }
        }
    }

    /**
     * cancel by tag
     */
    fun cancel(tag: String) {
        dispatcher.cancel(tag)
    }

    /**
     * close the connect：Effective in the case of reuse
     */
    fun close(url: String, code: Int = 0, reason: String? = null) {
        connectionPool().get(url)?.close(code, reason)
    }

}