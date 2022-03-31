package com.lizhi.component.net.xquic.listener

interface XquicCallback {
    fun callBack(ret: Int, data: ByteArray)
}