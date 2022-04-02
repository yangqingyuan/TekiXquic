package com.lizhi.component.net.xquic.mode

import java.lang.NullPointerException

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRequest {

    var url: String? = null
    var method: String = "GET"
    var headers: XHeaders.Builder? = null
    var body: XRequestBody? = null

    init {
        headers = XHeaders.Builder()
    }

    class Builder() {
        val xRequest = XRequest()

        fun build(): XRequest {
            return xRequest
        }

        fun url(url: String): Builder {
            if (url.isNullOrBlank()) {
                throw NullPointerException("url == null")
            }
            xRequest.url = url
            return this
        }


        fun header(name: String, value: String): Builder {
            xRequest.headers?.set(name, value)
            return this
        }

        fun addHeader(name: String, value: String): Builder {
            xRequest.headers?.add(name, value)
            return this
        }


        fun removeHeader(name: String, value: String): Builder {
            xRequest.headers?.set(name, value)
            return this
        }

        fun headers(heards: XHeaders): Builder {
            xRequest.headers = heards.newBuilder()
            return this
        }

        fun get(): Builder {
            xRequest.method = "GET"
            return this
        }

        fun post(xRequestBody: XRequestBody): Builder {
            xRequest.method = "POST"
            xRequest.body = xRequestBody
            return this
        }
    }
}