package com.lizhi.component.net.xquic.native


/**
 * 短链接
 */
object XquicShortNative {

    init {
        System.loadLibrary("xnet-lib")
        System.loadLibrary("xquic")
    }

    /**
     * 发送数据
     */
    external fun send(
        url: String,
        token: String?,
        session: String?,
        content: String
    ): Int

}