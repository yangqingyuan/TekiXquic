package com.lizhi.component.net.xquic.native

import com.lizhi.component.net.xquic.listener.XquicCallback

/**
 * 短链接
 */
class XquicShortNative {

    companion object {

        fun loadLib() {
            System.loadLibrary("xnet-lib")
            System.loadLibrary("xquic")
        }
    }

    init {
        loadLib()
    }

    /**
     * 发送数据
     */
    external fun send(
        url: String,
        token: String?,
        session: String?,
        content: String,
        xquicCallback: XquicCallback,
    ): Int
}