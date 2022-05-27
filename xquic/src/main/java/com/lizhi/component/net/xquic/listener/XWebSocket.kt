package com.lizhi.component.net.xquic.listener

import com.lizhi.component.net.xquic.impl.XRealWebSocket


/**
 * 作用: webSocket interface
 * 作者: yqy
 * 创建日期: 2022/4/17.
 */
interface XWebSocket {

    fun send(text: String): Boolean

    /**
     * 可以携带tag的方式，接受到数据返回,可以用与区分请求,每一个发送会转化成一个请求，跟短链接唯一的区别是共用一个链接
     */
    fun send(text: String, tag: String): Boolean

    fun send(message: XRealWebSocket.Message): Boolean

    fun cancel()
    fun close(code: Int, reason: String?)
}