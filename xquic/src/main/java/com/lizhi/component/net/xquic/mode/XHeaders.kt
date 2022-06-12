package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XHeaders(val headersMap: MutableMap<String, String>) {

    class Builder {
        private val headersMap = hashMapOf<String, String>()
        fun build(): XHeaders {
            return XHeaders(headersMap)
        }

        fun set(name: String, value: String) = apply {
            this.headersMap[name] = value
        }

        fun add(name: String, value: String) = apply {
            this.headersMap[name] = value
        }
    }

    fun newHeaders(): XHeaders {
        return XHeaders(headersMap)
    }
}