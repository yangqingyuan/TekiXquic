package com.example.xquicdemo

import android.content.Context
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XPingListener
import com.lizhi.component.net.xquic.quic.AlpnType
import com.lizhi.component.net.xquic.quic.CCType
import com.lizhi.component.net.xquic.quic.FinishFlag
import com.lizhi.component.net.xquic.utils.XLogUtils

object SetCache {

    private const val key = "tekixuqic"

    private const val KEY_CC_TYPE = "cc_type"
    private const val KEY_METHOD = "method"
    private const val KEY_URL = "url"
    private const val KEY_URL1 = "url1"
    private const val KEY_URL2 = "url2"
    private const val KEY_SELECT = "select"
    private const val KEY_REUSE = "reuse"
    private const val KEY_CONN_TIMEOUT = "conn_time_out"
    private const val KEY_TEST_COUNT = "conn_test_count"
    private const val KEY_TEST_SPACE = "conn_test_space"


    fun setCCType(context: Context, ccType: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_CC_TYPE, ccType).apply()
    }

    fun getCCType(context: Context): Int {
        return when (context.getSharedPreferences(key, 0)
            .getInt(KEY_CC_TYPE, CCType.BBR)) {
            CCType.BBR -> {
                CCType.BBR
            }
            CCType.CUBIC -> {
                CCType.CUBIC
            }
            else -> {
                CCType.RENO
            }
        }
    }


    fun setMethod(context: Context, getOrPost: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_METHOD, getOrPost).apply()
    }

    fun getMethod(context: Context): String {
        return context.getSharedPreferences(key, 0).getString(KEY_METHOD, "GET")!!
    }

    fun getSelectUrl(context: Context): String? {
        when (getSelect(context)) {
            0 -> {
                return getUrl(context)
            }
            1 -> {
                return getUrl1(context)
            }
            2 -> {
                return getUrl2(context)
            }
        }
        return null
    }


    fun getSelect(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_SELECT, 0)
    }

    fun setSelect(context: Context, index: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_SELECT, index).apply()
    }

    fun getReuse(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_REUSE, 0)
    }

    fun setReuse(context: Context, index: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_REUSE, index).apply()
    }


    fun setUrl(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL, url).apply()
    }

    fun getUrl(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL, "https://192.168.8.120:8443")
    }


    fun setUrl1(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL1, url).apply()
    }

    fun getUrl1(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL1, "")
    }


    fun setUrl2(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL2, url).apply()
    }

    fun getUrl2(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL2, "")
    }


    fun setConnTimeOut(context: Context, timeout: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_CONN_TIMEOUT, timeout).apply()
    }

    fun getConnTimeout(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_CONN_TIMEOUT, 10)
    }


    fun setTestCount(context: Context, count: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_TEST_COUNT, count).apply()
    }

    fun getTestCount(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_TEST_COUNT, 1)
    }

    fun getTestSpace(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_TEST_SPACE, 0)
    }

    fun setTestSpace(context: Context, count: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_TEST_SPACE, count).apply()
    }

    var xquicClient: XquicClient? = null
    fun getClient(context: Context): XquicClient {
        if (xquicClient == null) {
            xquicClient = XquicClient.Builder()
                .connectTimeOut(getConnTimeout(context))
                .setReadTimeOut(getConnTimeout(context))
                .ccType(getCCType(context))//可选：拥塞算法，默认CUBIC
                .pingInterval(5000,10)//可选：ping的时间间隔和超时次数，单位毫秒，如果大于0，则发送心跳，默认不发生
                //.setFinishFlag(FinishFlag.FINISH)//是否结束流，只对HQ，webSocket场景有效，每次发送数据后不会关闭stream，下次发送会继续使用已有的stream，以达到stream复用
                //.setCryptoFlag(CryptoFlag.WITHOUT_CRYPTO)//可选：是否加密，默认加密
                //.setProtoVersion(ProtoVersion.XQC_IDRAFT_VER_29)//根据服务端端支持协议切换，默认H3
                //.setAlpnType(AlpnType.ALPN_HQ)//支持协议切换，默认H3
                //.setAlpnName("hq-interop")//自定义协议名字，注意：针对ALPN_HQ生效，要跟服务端对应，避免链接不上的问题
                .setContext(context) //可选：如果设置了，会内部会进行网络切换监听
                //更多参数可以看详细的文档或者代码
                .addPingListener(object : XPingListener {//可选，可以自定义ping内容，或者使用sdk内部默认值"ping"
                    override fun ping(): ByteArray {
                        return "ping data".toByteArray()
                    }

                    override fun pong(data: ByteArray?) {
                        XLogUtils.info("data=${String(data!!)}")
                    }
                })
                .build()
        }
        return xquicClient!!
    }
}