package com.lizhi.component.net.xquic.native

import java.nio.ByteBuffer

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
     * 发送byte数据
     * dataType : 1 jsonString, 2 byteArray
     * 注意：
     * （1）ByteBuffer是使用allocateDirect的方式，内存共享更加高效，通过profiler（内存分析）并发100请求，内存峰值减少50%
     * （2）jsonString类型必须遵守SendBody的格式（因：内部需要）
     */
    external fun sendByte(clientCtx: Long, dataType: Int, byte: ByteBuffer, len: Int): Int

    /**
     * 取消
     */
    external fun cancel(clientCtx: Long): Int
}