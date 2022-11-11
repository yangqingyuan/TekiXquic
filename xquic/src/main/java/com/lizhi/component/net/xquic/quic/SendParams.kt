package com.lizhi.component.net.xquic.quic


/**
 * 作用: native send params
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class SendParams(val builder: Builder) {
    /**
     * key param
     */
    var url: ByteArray = builder.url.toByteArray()
    var urlLen: Int = url.size

    /**
     * optional param
     */
    var token: ByteArray? = builder.token
    var tokenLen: Int = builder.token?.size ?: 0

    /**
     * optional param
     */
    var session: ByteArray? = builder.session
    var sessionLen: Int = builder.session?.size ?: 0

    /**
     * post content
     */
    var content: ByteArray? = builder.content
    var contentLength = builder.contentLength
    var dataType: Int = builder.dataType

    /**
     * optional param
     * unit：second
     */
    var connectTimeOut: Int = builder.connectTimeOut

    /**
     * optional param
     * unit：second
     */
    var readTimeOut: Int = builder.readTimeOut

    /**
     * optional param
     * default: 1M
     */
    var maxRecvDataLen: Int = builder.maxRecvDataLen

    /**
     * optional param
     * default: bbr
     */
    var ccType: Int = builder.ccType

    /**
     * common head
     */
    val headers = builder.headers

    /**
     * commonHeaders size
     */
    var headersSize = builder.headersSize

    /**
     * proto version
     */
    var protoVersion: Int = builder.protoVersion

    /**
     * alpn type
     */
    var alpnType = builder.alpnType
    var alpnName: ByteArray = builder.alpnName.toByteArray()
    var alpnLen = alpnName.size

    /**
     * 0: crypto
     * 1: 1:without crypto
     */
    var cryptoFlag = builder.cryptoFlag

    /**
     * request finish flag, 1 for finish.
     */
    var finishFlag = builder.finishFlag

    class Builder {

        /**
         * key param,must not null
         */
        lateinit var url: String

        /**
         * optional param
         */
        internal var token: ByteArray? = null

        /**
         * optional param
         */
        internal var session: ByteArray? = null

        /**
         * post content
         */
        var content: ByteArray? = null
        var contentLength = 0
        var dataType: Int = DataType.JSON

        /**
         * optional param
         * unit：second
         */
        internal var connectTimeOut: Int = 30

        /**
         * optional param
         * unit：second
         */
        internal var readTimeOut: Int = 30

        /**
         * optional param
         * default: 1M
         */
        internal var maxRecvDataLen: Int = 1024 * 1024

        /**
         * optional param
         * default: bbr
         */
        internal var ccType: Int = CCType.BBR

        /**
         * common head
         */
        internal val headers = java.util.HashMap<String, String>()

        /**
         * commonHeaders size
         */
        internal var headersSize = 0

        /**
         * proto version
         */
        internal var protoVersion: Int = ProtoVersion.XQC_VERSION_V1

        /**
         * alpn type
         */
        internal var alpnType = AlpnType.ALPN_H3
        internal var alpnName: String = "transport"

        /**
         * 0: crypto
         * 1: 1:without crypto
         */
        internal var cryptoFlag = CryptoFlag.CRYPTO

        /**
         * request finish flag, 1 for finish.
         */
        internal var finishFlag = FinishFlag.FINISH


        fun setUrl(url: String) = apply {
            this.url = url
        }

        fun setToken(token: ByteArray?) = apply {
            this.token = token
        }

        fun setSession(session: ByteArray?) = apply {
            this.session = session
        }

        fun setContent(content: ByteArray) = apply {
            this.content = content
        }

        fun setContentLength(contentLength: Int) = apply {
            this.contentLength = contentLength
        }

        fun setDataType(dataType: Int) = apply {
            this.dataType = dataType
        }

        fun setConnectTimeOut(timeOut: Int) = apply {
            this.connectTimeOut = timeOut
        }

        fun setReadTimeOut(timeOut: Int) = apply {
            this.readTimeOut = timeOut
        }

        fun setMaxRecvLenght(length: Int) = apply {
            this.maxRecvDataLen = length
        }

        fun setCCType(@CCType.Type ccType: Int) = apply {
            this.ccType = ccType
        }

        fun setProtoVersion(@ProtoVersion.Version version: Int) = apply {
            this.protoVersion = version
        }

        fun setHeaders(headers: HashMap<String, String>) = apply {
            if (headers.isNotEmpty()) {
                this.headers.putAll(headers)
            }
        }

        fun setFinishFlag(@FinishFlag.Type flag: Int) = apply {
            this.finishFlag = flag
        }

        fun setAlpnType(@AlpnType.Type type: Int) = apply {
            this.alpnType = type
        }

        fun setCryptoFlag(@CryptoFlag.Type type: Int) = apply {
            this.alpnType = type
        }

        fun setAlpnName(alpnName: String) = apply {
            this.alpnName = alpnName
        }

        fun build(): SendParams {
            this.headersSize = this.headers.size
            return SendParams(this)
        }
    }

    override fun toString(): String {
        return "SendParams(url=$url, token=$token, session=$session, content=$content, timeOut=$connectTimeOut, maxRecvDataLen=$maxRecvDataLen, ccType=$ccType)"
    }

}