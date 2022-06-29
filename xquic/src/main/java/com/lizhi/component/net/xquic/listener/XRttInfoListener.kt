package com.lizhi.component.net.xquic.listener

interface XRttInfoListener {
    fun getToken(url: String): ByteArray?
    fun getSession(url: String): ByteArray?

    fun tokenBack(url:String,byteArray: ByteArray?)
    fun sessionBack(url:String,byteArray: ByteArray?)
}