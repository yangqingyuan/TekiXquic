package com.lizhi.component.net.xquic.listener

import com.lizhi.component.net.xquic.mode.XResponse

/**
 * 作用: webSocket listener interface
 * 作者: yqy
 * 创建日期: 2022/4/17.
 */
interface XWebSocketListener {
    fun onOpen(webSocket: XWebSocket, response: XResponse)

    fun onMessage(webSocket: XWebSocket, data: ByteArray)

    fun onFailure(webSocket: XWebSocket, t: Throwable, response: XResponse)
}