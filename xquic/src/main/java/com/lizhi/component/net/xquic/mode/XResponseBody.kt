package com.lizhi.component.net.xquic.mode

import java.io.Closeable

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponseBody(var data: ByteArray?) : Closeable {

    fun getData(): String? {
        data?.let {
            return String(it)
        }
        return null
    }

    override fun close() {

    }
}