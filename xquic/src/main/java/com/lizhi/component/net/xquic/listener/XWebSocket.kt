package com.lizhi.component.net.xquic.listener


/**
 * 作用: webSocket interface
 * 作者: yqy
 * 创建日期: 2022/4/17.
 */
interface XWebSocket {
    fun send(text: String): Boolean
    fun cancel()
    fun close(code: Int, reason: String?)
}