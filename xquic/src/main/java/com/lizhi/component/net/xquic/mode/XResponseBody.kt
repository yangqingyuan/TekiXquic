package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponseBody(
    var data: ByteArray,
    /**
     * 注意成功的时候返回tag，其他状态返回null
     */
    var tag: String? = null
) {

    fun body(): String {
        return String(data)
    }

    fun byteBody(): ByteArray {
        return data
    }
}