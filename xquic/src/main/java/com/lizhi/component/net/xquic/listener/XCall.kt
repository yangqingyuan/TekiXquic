package com.lizhi.component.net.xquic.listener

import com.lizhi.component.net.xquic.mode.XRequest

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
interface XCall {

    fun request(): XRequest

    fun enqueue(xCallback: XCallBack?)

    fun cancel()

    fun isExecuted(): Boolean

    fun isCanceled(): Boolean
}