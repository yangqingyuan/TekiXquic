package com.lizhi.component.net.xquic.mode

interface XChain {
    fun request(): XRequest
    fun proceed(request: XRequest): XResponse
}