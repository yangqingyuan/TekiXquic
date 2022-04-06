package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/6
 */
class XHttpUrl {

    companion object {

        fun defaultPort(scheme: String): Int {
            return when (scheme) {
                "http" -> {
                    80
                }
                "https" -> {
                    443
                }
                else -> {
                    8443
                }
            }
        }

        fun get(url: String): XHttpUrl {
            return Builder().parse(url).build()
        }
    }


    var scheme: String? = null
    var host: String? = null
    var port: Int = 0
    var url: String? = null
    val pathSegments by lazy { mutableListOf<String>() }

    class Builder {
        private val xHttpUrl = XHttpUrl()

        fun parse(input: String): Builder {
            xHttpUrl.url = input

            return this
        }

        fun build(): XHttpUrl {
            return xHttpUrl
        }
    }

}
