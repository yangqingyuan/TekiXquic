package com.lizhi.component.net.xquic.native

import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception

/**
 * 长链接
 */
class XquicLongNative {

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

    private var clientCtx: Long = 0L

    /**
     * 链接
     * return 返回的是clientCtx的指针地址，为其他函数提供入参
     */
    external fun connect(
        param: SendParams,
        xquicCallback: XquicCallback
    ): Long

    /**
     * 开始
     */
    external fun start(clientCtx: Long): Int

    /**
     * 发送ping数据
     */
    external fun sendPing(clientCtx: Long, content: String): Int

    /**
     * 发送数据
     */
    external fun send(clientCtx: Long, content: String): Int

    /**
     * 取消
     */
    external fun cancel(clientCtx: Long): Int
}