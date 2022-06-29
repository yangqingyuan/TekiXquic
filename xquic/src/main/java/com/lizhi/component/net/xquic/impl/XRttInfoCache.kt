package com.lizhi.component.net.xquic.impl

import android.util.LruCache
import com.lizhi.component.net.xquic.listener.XRttInfoListener

/**
 * 作用: 0-rtt conn info
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRttInfoCache : XRttInfoListener {
    /**
     * token
     */
    private val tokenMap = LruCache<String, ByteArray>(16)

    /**
     * session
     */
    private val sessionMap = LruCache<String, ByteArray>(16)


    override fun getToken(url: String): ByteArray? {
        return tokenMap.get(url)
    }

    override fun getSession(url: String): ByteArray? {
        return sessionMap.get(url)
    }

    override fun tokenBack(url: String, byteArray: ByteArray?) {
        byteArray?.let {
            tokenMap.put(url, byteArray)
        }
    }

    override fun sessionBack(url: String, byteArray: ByteArray?) {
        byteArray?.let {
            sessionMap.put(url, byteArray)
        }
    }
}