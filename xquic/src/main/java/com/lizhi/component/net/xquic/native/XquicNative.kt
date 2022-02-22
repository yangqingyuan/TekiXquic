package com.lizhi.component.net.xquic.native

/**
 * xquic 引擎native层调用
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/2/21.
 */
object XquicNative {
    init {
        System.loadLibrary("xnet-lib")
        System.loadLibrary("xquic")
        System.loadLibrary("ev")
    }

    /**
     * 初始化
     * return 返回的是clientCtx的指针地址，为其他函数提供入参
     */
    external fun xquicInit(): Long

    /**
     * 开始链接
     * host：所要链接发送数据的ip地址
     * port：所有链接发送数据的端口号
     * token：链接的token，首次链接可以为null，底层用于0-RTT和校验
     * session：链接session，首次链接可以为null，底层用于0-RTT和校验
     * return >=0 成功，其他异常
     */
    external fun xquicConnect(
        clientCtx: Long,
        host: String,
        port: Int,
        token: String?,
        session: String?
    ): Int

    /**
     * 开始
     */
    external fun xquicStart(clientCtx: Long): Int

    /**
     * 发送数据
     * 要先调用xquicStart，才能发送数据
     */
    external fun xquicSend(clientCtx: Long, content: String): Int

    /**
     * 发送数据
     * H3的方式发送数据
     * 要先调用xquicStart，才能发送数据
     */
    external fun xquicH3Send(clientCtx: Long, content: String): Int

    /**
     * 销毁
     */
    external fun xquicDestroy(clientCtx: Long): Int
}