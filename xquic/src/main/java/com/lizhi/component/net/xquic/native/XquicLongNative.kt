package com.lizhi.component.net.xquic.native

/**
 * 长链接
 */
object XquicLongNative {

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