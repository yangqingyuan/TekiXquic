package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponse {
    var xRequest: XRequest? = null
    var code: Int = 0
    var message: String? = null

    var xHeaders: XHeaders? = null
    lateinit var xResponseBody: XResponseBody


    class Builder() {
        private val xResponse = XResponse()

        fun request(xRequest: XRequest?): Builder {
            xResponse.xRequest = xRequest
            return this
        }

        fun headers(xHeaders: XHeaders?): Builder {
            xResponse.xHeaders = xHeaders
            return this
        }

        fun responseBody(xResponseBody: XResponseBody): Builder {
            xResponse.xResponseBody = xResponseBody
            return this
        }

        fun build(): XResponse {
            return xResponse
        }

    }

}