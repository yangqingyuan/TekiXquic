package com.lizhi.component.net.xquic.native

/**
 * 长链接
 */
class XquicLongNative {

    init {
        XquicLoader.loadLib()
    }


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
     * 发送带头的
     * 用于复用链接
     */
    external fun sendWithHead(clientCtx: Long, param: SendParams, content: String): Int


    /**
     * 取消
     */
    external fun cancel(clientCtx: Long): Int
}