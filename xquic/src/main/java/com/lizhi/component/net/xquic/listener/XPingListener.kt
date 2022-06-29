package com.lizhi.component.net.xquic.listener

/**
 * 作用: webSocket ping/pong
 * 作者: yqy
 * 创建日期: 2022/4/18.
 */
interface XPingListener {
    fun ping(): String
    fun pong(data: ByteArray?)
}