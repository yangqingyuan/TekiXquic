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

    const val SEND_TYPE_H3:Int = 1 //发送方式，以H3的方式发送
    const val SEND_TYPE_XQUIC:Int = 2 //以xquic，的方式透传


    /**
     * 初始化
     * host：所要链接发送数据的ip地址
     * port：所有链接发送数据的端口号
     * token：链接的token，首次链接可以为null，底层用于0-RTT和校验
     * session：链接session，首次链接可以为null，底层用于0-RTT和校验
     * return 返回的是clientCtx的指针地址，为其他函数提供入参
     */
    external fun xquicInit(host: String, port: Int, token: String?, session: String?): Long

    /**
     * 开始
     * 要先调用xquicInit进行初始化
     * 然后在调用
     */
    external fun xquicStart(clientCtx: Long): Int

    /**
     * 发送数据
     * 要先调用xquicStart，才能发送数据
     */
    external fun xquicSend(clientCtx: Long, type: Int, content: String): Int

    /**
     * 销毁
     */
    external fun xquicDestroy(clientCtx: Long): Int
}