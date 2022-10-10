package com.lizhi.component.net.xquic.quic


/**
 * 短链接
 */
class XquicShortNative {

    init {
        XquicLoader.loadLib()
    }

    /**
     * 发送数据
     */
    external fun send(
        param: SendParams,
        xquicCallback: XquicCallback,
    ): Int

    /**
     * cancel
     */
    external fun cancel(clientCtx: Long): Int
}