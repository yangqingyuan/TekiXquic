package com.lizhi.component.net.xquic.native

import com.lizhi.component.net.xquic.listener.XquicCallback

/**
 * 短链接
 */
class XquicShortNative {

    companion object {

        fun loadLib() {
            System.loadLibrary("xnet-lib")
            System.loadLibrary("xquic")
        }
    }


    /**
     * 拥塞算法
     */
    enum class CCType {
        BBR,
        CUBIC,
        RENO
    }

    open class SendParams {
        var url: String? = null // key param
        var token: String? = null // optional param
        var session: String? = null // optional param
        var content: String? = null //key param
        var timeOut: Int = 30 //optional default: 30 second
        var maxRecvDataLen: Int = 1024 * 1024 //optional default: 1M
        var ccType: Int = CCType.BBR.ordinal//optional

        open class Builder {
            private val params = SendParams()

            fun setUrl(url: String): Builder {
                params.url = url
                return this
            }

            fun setToken(token: String?): Builder {
                params.token = token
                return this
            }

            fun setSession(session: String?): Builder {
                params.session = session
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

            fun build(): SendParams {
                return params
            }
        }
    }


    init {
        loadLib()
    }

    /**
     * 发送数据
     */
    external fun send(
        param: SendParams,
        xquicCallback: XquicCallback,
    ): Int
}