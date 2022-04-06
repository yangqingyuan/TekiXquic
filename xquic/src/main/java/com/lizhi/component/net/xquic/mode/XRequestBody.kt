package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRequestBody {
    lateinit var content: String
    lateinit var mediaType: XMediaType

    companion object {
        fun create(mediaType: XMediaType, content: String): XRequestBody {
            val xRequestBody = XRequestBody()
            xRequestBody.content = content
            xRequestBody.mediaType = mediaType
            return xRequestBody
        }
    }
}