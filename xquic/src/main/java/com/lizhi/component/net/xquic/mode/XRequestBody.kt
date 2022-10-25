package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRequestBody {
    lateinit var content: Any
    lateinit var mediaType: XMediaType
    private var contentLength = 0

    companion object {

        /**
         * create default String body
         */
        fun createDefaultBody(content: String): XRequestBody {
            return createStringBody(XMediaType.MEDIA_TYPE_TEXT, content)
        }

        /**
         * create default byteArray body
         */
        fun createDefaultBody(content: ByteArray): XRequestBody {
            return createByteArrayBody(XMediaType.MEDIA_TYPE_MULTIPART, content)
        }

        /**
         * create String body
         */
        fun createStringBody(@XMediaType.Type mediaType: String, content: String): XRequestBody {
            return create(XMediaType.parse(mediaType), content)
        }

        /**
         * create Byte body
         */
        fun createByteArrayBody(
            @XMediaType.Type mediaType: String,
            content: ByteArray
        ): XRequestBody {
            return create(XMediaType.parse(mediaType), content)
        }

        /**
         * create string boy
         */
        fun create(mediaType: XMediaType, content: String): XRequestBody {
            val xRequestBody = XRequestBody()
            xRequestBody.content = content
            xRequestBody.mediaType = mediaType
            xRequestBody.contentLength = content.length
            return xRequestBody
        }

        /**
         * create Byte body
         */
        fun create(mediaType: XMediaType, content: ByteArray): XRequestBody {
            val xRequestBody = XRequestBody()
            xRequestBody.content = content
            xRequestBody.mediaType = mediaType
            xRequestBody.contentLength = content.size
            return xRequestBody
        }
    }

    /**
     * getString
     */
    fun getContentString(): String {
        return if (content is String) {
            content as String
        } else {
            String(content as ByteArray)
        }
    }


    /**
     * getByte
     */
    fun getContentByteArray(): ByteArray {
        return if (content is ByteArray) {
            content as ByteArray
        } else {
            (content as String).toByteArray()
        }
    }

    /**
     * get content length
     */
    fun getContentLength(): Int {
        if (contentLength > 0) {
            return contentLength
        }
        contentLength = getContentByteArray().size
        return contentLength
    }

    /**
     * get media type string
     */
    fun getMediaType(): String {
        return mediaType.mediaType
    }
}