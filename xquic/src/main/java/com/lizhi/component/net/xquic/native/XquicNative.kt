package com.lizhi.component.net.xquic.native

/**
 *
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/2/21.
 */
object XquicNative {

    /**
     * 初始化
     */
    external fun init(host:String,port:Int)

    /**
     * 发送数据
     */
    external fun send(content:String)

}