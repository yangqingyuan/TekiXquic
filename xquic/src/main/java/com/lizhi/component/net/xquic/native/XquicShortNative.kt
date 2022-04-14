package com.lizhi.component.net.xquic.native

import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception

/**
 * 短链接
 */
class XquicShortNative {

    companion object {
        var libLoaded: Boolean = false
        fun loadLib() {
            try {
                synchronized(this) {
                    if (!libLoaded) {
                        System.loadLibrary("xnet-lib")
                        System.loadLibrary("xquic")
                        libLoaded = true
                    }
                }
            } catch (e: Exception) {
                XLogUtils.error(e)
            }
        }
    }

    init {
        loadLib()
    }

    /**
     * 发送数据
     */
    external fun send(
        param: SendParams,
        xquicCallback: XquicCallback,
    ): Int
}