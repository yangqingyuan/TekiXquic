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

    /**
     * 链接
     * return 返回的是clientCtx的指针地址，为其他函数提供入参
     */
    external fun connect(
        host: String,
        port: Int,
        token: String?,
        session: String?
    ): Long

    /**
     * 发送数据
     */
    external fun send(clientCtx: Long, content: String): Int

    /**
     * 取消
     */
    external fun cancel(clientCtx: Long): Int
}