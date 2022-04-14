package com.lizhi.component.net.xquic.native


/**
 * 作用: native send params
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class SendParams {
    /**
     * key param
     */
    var url: String? = null

    /**
     * optional param
     */
    var token: String? = null

    /**
     * optional param
     */
    var session: String? = null

    /**
     * post content
     */
    var content: String? = null

    /**
     * optional param
     * unit：second
     */
    var timeOut: Int = 30

    /**
     * optional param
     * default: 1M
     */
    var maxRecvDataLen: Int = 1024 * 1024

    /**
     * optional param
     * default: bbr
     */
    var ccType: Int = CCType.BBR.ordinal

    /**
     * common head
     */
    val headers = java.util.HashMap<String, String>()

    /**
     * commonHeaders size
     */
    var headersSize = 0


    open class Builder {
        private val params = SendParams()

        fun setUrl(url: String): Builder {
            params.url = url
            return this
        }

        fun setToken(token: String?): Builder {
            if (!token.isNullOrBlank()) {
                params.token = token
            }
            return this
        }

        fun setSession(session: String?): Builder {
            if (!session.isNullOrBlank()) {
                params.session = session
            }
            return this
        }

        fun setContent(content: String): Builder {
            params.content = content
            return this
        }

        fun setTimeOut(timeOut: Int): Builder {
            params.timeOut = timeOut
            return this
        }

        fun setMaxRecvLenght(length: Int): Builder {
            params.maxRecvDataLen = length
            return this
        }

        fun setCCType(ccType: CCType): Builder {
            params.ccType = ccType.ordinal
            return this
        }

        fun setHeaders(headers: HashMap<String, String>): Builder {
            if (headers.isNotEmpty()) {
                params.headers.putAll(headers)
            }
            return this
        }

        fun build(): SendParams {
            params.headersSize = params.headers.size
            return params
        }
    }

    override fun toString(): String {
        return "SendParams(url=$url, token=$token, session=$session, content=$content, timeOut=$timeOut, maxRecvDataLen=$maxRecvDataLen, ccType=$ccType)"
    }

}