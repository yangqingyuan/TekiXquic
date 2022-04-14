package com.lizhi.component.net.xquic.listener

import com.lizhi.component.net.xquic.mode.XChain
import com.lizhi.component.net.xquic.mode.XResponse

/**
 * 作用: 拦截器
 * 作者: yqy
 * 创建日期: 2022/4/14.
 */
interface XInterceptor {
    fun intercept(chain: XChain): XResponse
}