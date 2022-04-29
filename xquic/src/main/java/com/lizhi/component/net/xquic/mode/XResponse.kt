package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponse {
    lateinit var xRequest: XRequest
    lateinit var xHeaders: XHeaders
    lateinit var xResponseBody: XResponseBody

    var code: Int = 0
    var message: String? = null

    /**
     * queue wait time，unit ms
     */
    var delayTime = 0L

    var index = 0


    class Builder() {
        private val xResponse = XResponse()

        fun request(xRequest: XRequest): Builder {
            xResponse.xRequest = xRequest
            return this
        }

        fun headers(xHeaders: XHeaders): Builder {
            xResponse.xHeaders = xHeaders
            return this
        }

        fun delayTime(delayTime: Long): Builder {
            xResponse.delayTime = delayTime
            return this
        }

        fun index(index: Int): Builder {
            xResponse.index = index
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

    fun getStatus(): Int {
        val status = xHeaders.headersMap[":status"]
        if (!status.isNullOrBlank()) {
            return status.toInt()
        }
        return -1
    }

    fun getContentType(): String? {
        return xHeaders.headersMap["content-type"]
    }

    fun getContentLength(): Int {
        val status = xHeaders.headersMap["content-length"]
        if (!status.isNullOrBlank()) {
            return status.toInt()
        }
        return 0
    }

    fun getValue(key: String): String? {
        return xHeaders.headersMap[key]
    }


}