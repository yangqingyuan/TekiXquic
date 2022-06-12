package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponse(builder: Builder) {
    var xRequest: XRequest = builder.xRequest
    var xHeaders: XHeaders = builder.xHeaders
    lateinit var xResponseBody: XResponseBody

    var code: Int = builder.code
    var message: String? = builder.message

    /**
     * queue wait time，unit ms
     */
    var delayTime = builder.delayTime

    var index = builder.index

    init {
        builder.xResponseBody?.let {
            xResponseBody = it
        }
    }


    class Builder() {
        var code: Int = 0
        var message: String? = null

        /**
         * queue wait time，unit ms
         */
        var delayTime = 0L

        var index = 0

        lateinit var xRequest: XRequest
        lateinit var xHeaders: XHeaders
        var xResponseBody: XResponseBody? = null

        fun request(xRequest: XRequest) = apply {
            this.xRequest = xRequest
        }

        fun headers(xHeaders: XHeaders) = apply {
            this.xHeaders = xHeaders
        }

        fun delayTime(delayTime: Long) = apply {
            this.delayTime = delayTime
        }

        fun index(index: Int) = apply {
            this.index = index
        }

        fun responseBody(xResponseBody: XResponseBody) = apply {
            this.xResponseBody = xResponseBody
        }

        fun build(): XResponse {
            return XResponse(this)
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