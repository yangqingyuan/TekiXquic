package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicShortNative

/**
 * 作用: 链接
 * 作者: yqy
 * 创建日期: 2022/5/26.
 */
class XConnection {

    private var clientCtx: Long = 0L

    /**
     * short native
     */
    private val xquicShortNative = XquicShortNative()


    fun send(params: SendParams) {

        xquicShortNative.send(params, object : XquicCallback {
            override fun callBackData(ret: Int, data: String) {

            }

            override fun callBackMessage(msgType: Int, data: String) {
            }

        })
    }
}