package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XHeaders {

    val headersMap by lazy { hashMapOf<String, String>() }

    class Builder {
        private val xHeaders = XHeaders()

        fun build(): XHeaders {
            return xHeaders
        }

        fun set(name: String, value: String) {
            xHeaders.headersMap[name] = value
        }

        fun add(name: String, value: String) {
            xHeaders.headersMap[name] = value
        }
    }

    fun newHeaders():XHeaders {
        val xHeaders = XHeaders()
        xHeaders.headersMap.putAll(headersMap)
        return xHeaders
    }
}