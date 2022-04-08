package com.lizhi.component.net.xquic.mode


/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponseBody(var data: ByteArray) {

    fun getData(): String {
        return String(data)
    }

}