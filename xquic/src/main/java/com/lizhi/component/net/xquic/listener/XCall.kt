package com.lizhi.component.net.xquic.listener

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
interface XCall {
    fun enqueue(xCallback: XCallBack?)
}