package com.lizhi.component.net.xquic.listener

import com.lizhi.component.net.xquic.impl.XRealWebSocket


/**
 * 作用: webSocket interface
 * 作者: yqy
 * 创建日期: 2022/4/17.
 */
interface XWebSocket {

    /**
     * 发送test
     */
    fun send(text: String): Boolean

    /**
     * 可以携带tag的方式，接受到数据返回,可以用于区分请求,每一个发送会转化成一个请求，跟短链接唯一的区别是共用一个链接
     */
    fun send(text: String, tag: String): Boolean

    /**
     * 发送byte数组
     */
    fun send(byteArray: ByteArray): Boolean

    /**
     * 发送消息，不管byteArray/string，内部都是统一转成message
     */
    fun send(message: XRealWebSocket.Message): Boolean

    /**
     * result on : onFailed
     * 跟close的唯一区别是cancel会在onFailed中返回
     */
    fun cancel()

    /**
     * result on : onClose
     * 跟cancel的唯一区别是close会在onClose，将传递的参数逐一返回
     */
    fun close(code: Int, reason: String?)

    /**
     * 判断是否已经关闭
     */
    fun isClose(): Boolean
}