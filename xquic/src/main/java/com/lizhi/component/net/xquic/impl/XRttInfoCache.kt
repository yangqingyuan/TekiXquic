package com.lizhi.component.net.xquic.impl

import android.util.LruCache

/**
 * 作用: 0-rtt conn info
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
object XRttInfoCache {
    /**
     * token
     */
    val tokenMap by lazy { LruCache<String, String>(100) }

    /**
     * session
     */
    val sessionMap by lazy { LruCache<String, String>(100) }

    /**
     * tp
     */
    val tpMap by lazy { LruCache<String, String>(100) }
}