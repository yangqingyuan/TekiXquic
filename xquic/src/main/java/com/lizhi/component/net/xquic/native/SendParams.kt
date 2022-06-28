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
    var connectTimeOut: Int = 30

    /**
     * optional param
     * unit：second
     */
    var readTimeOut: Int = 30

    /**
     * optional param
     * default: 1M
     */
    var maxRecvDataLen: Int = 1024 * 1024

    /**
     * optional param
     * default: bbr
     */
    var ccType: Int = CCType.BBR

    /**
     * common head
     */
    val headers = java.util.HashMap<String, String>()

    /**
     * commonHeaders size
     */
    var headersSize = 0

    /**
     * proto version
     */
    var protoVersion: Int = ProtoVersion.XQC_VERSION_V1

    /**
     * alpn type
     */
    var alpnType = AlpnType.ALPN_H3

    open class Builder {
        private val params = SendParams()

        fun setUrl(url: String) = apply {
            params.url = url
        }

        fun setToken(token: String?) = apply {
            if (!token.isNullOrBlank()) {
                params.token = token
            }
        }

        fun setSession(session: String?) = apply {
            if (!session.isNullOrBlank()) {
                params.session = session
            }
        }

        fun setContent(content: String) = apply {
            params.content = content
        }

        fun setConnectTimeOut(timeOut: Int) = apply {
            params.connectTimeOut = timeOut
        }

        fun setReadTimeOut(timeOut: Int) = apply {
            params.readTimeOut = timeOut
        }

        fun setMaxRecvLenght(length: Int) = apply {
            params.maxRecvDataLen = length
        }

        fun setCCType(@CCType.Type ccType: Int) = apply {
            params.ccType = ccType
        }

        fun setProtoVersion(@ProtoVersion.Version version: Int) = apply {
            params.protoVersion = version
        }

        fun setHeaders(headers: HashMap<String, String>) = apply {
            if (headers.isNotEmpty()) {
                params.headers.putAll(headers)
            }
        }

        fun setAlpnType(@AlpnType.Type type: Int) = apply {
            params.alpnType = type
        }

        fun build(): SendParams {
            params.headersSize = params.headers.size
            return params
        }
    }

    override fun toString(): String {
        return "SendParams(url=$url, token=$token, session=$session, content=$content, timeOut=$connectTimeOut, maxRecvDataLen=$maxRecvDataLen, ccType=$ccType)"
    }

}