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
     * 发送数据，适合http请求
     */
    external fun send(clientCtx: Long, json: String): Int

    /**
     * 发送byte数据，适合传递音视频数据
     */
    external fun sendByte(clientCtx: Long, byte: ByteArray): Int

    /**
     * 取消
     */
    external fun cancel(clientCtx: Long): Int
}