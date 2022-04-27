package com.lizhi.component.net.xquic.mode

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/6
 */
class XHttpUrl {

    companion object {

        private fun skipLeadingAsciiWhitespace(input: String, pos: Int, limit: Int): Int {
            for (i in pos until limit) {
                return when (input[i]) {
                    '\t', '\n', '\r', ' ' -> continue
                    else -> i
                }
            }
            return limit
        }

        private fun schemeDelimiterOffset(input: String, pos: Int, limit: Int): Int {
            if (limit - pos < 2) return -1
            val c0 = input[pos]
            if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) return -1 // Not a scheme start char.
            for (i in pos + 1 until limit) {
                val c = input[i]
                return if (c in 'a'..'z'
                    || c in 'A'..'Z'
                    || c in '0'..'9'
                    || c == '+' || c == '-' || c == '.'
                ) {
                    continue  // Scheme character. Keep going.
                } else if (c == ':') {
                    i // Scheme prefix!
                } else {
                    -1 // Non-scheme character before the first ':'.
                }
            }
            return -1 // No ':'; doesn't start with a scheme.
        }

        private fun skipTrailingAsciiWhitespace(input: String, pos: Int, limit: Int): Int {
            for (i in limit - 1 downTo pos) {
                return when (input[i]) {
                    '\t', '\n', '\r', ' ' -> continue
                    else -> i + 1
                }
            }
            return pos
        }

        fun get(url: String): XHttpUrl {
            return Builder().parse(url).build()
        }
    }

    /**
     * http/https
     */
    lateinit var scheme: String


    /**
     * sample:https://192.168.10.21:8443/test?gws_rd=ssl
     * path = /test?gws_rd=ssl
     */
    var path: String? = null

    /**
     * sample:https://192.168.10.21:8443/test?gws_rd=ssl
     * host = 192.168.10.21
     */
    lateinit var host: String
    lateinit var url: String
    var port: Int = 0

    /**
     * sample:https://www.google.com.hk/?gws_rd=ssl
     * authority = www.google.com.hk
     */
    lateinit var authority: String

    //private val pathSegments by lazy { mutableListOf<String>() }

    class Builder {
        private val xHttpUrl = XHttpUrl()

        /**
         * https://ip:host/path?xx=xxx
         */
        fun parse(url: String): Builder {
            xHttpUrl.url = url
            var pos: Int = skipLeadingAsciiWhitespace(url, 0, url.length)
            val limit: Int = skipTrailingAsciiWhitespace(url, pos, url.length)

            // Scheme.
            val schemeDelimiterOffset: Int = schemeDelimiterOffset(url, pos, limit)

            when {
                url.regionMatches(0, "https:", 0, 6) -> {
                    xHttpUrl.scheme = "https"
                    pos += "https://".length
                }
                url.regionMatches(0, "http:", 0, 5) -> {
                    xHttpUrl.scheme = "http"
                    pos += "http://".length
                }
                else -> {
                    throw IllegalArgumentException(
                        "Expected URL scheme 'http' or 'https' but was '"
                                + url.substring(0, schemeDelimiterOffset) + "'"
                    )
                }
            }

            /* parse host */
            val temp = url.substring(pos, limit)
            var list = if (temp.contains(":")) {
                temp.split(":")
            } else {
                temp.split("/")
            }
            if (list.isNotEmpty()) {
                xHttpUrl.host = list[0]
                xHttpUrl.authority = list[0]
            } else {
                throw IllegalArgumentException(
                    "Expected URL host url:$url"
                )
            }

            /* parse port */
            pos += xHttpUrl.host.length + 1
            if (pos <= limit) {
                list = url.substring(pos, limit).split("/")
                if (list.isNotEmpty()) {
                    try {
                        xHttpUrl.port = list[0].toInt()
                        pos += list[0].length
                    } catch (e: Exception) {
                        pos -= 1
                    }
                }
            }

            if (xHttpUrl.port <= 0) {
                xHttpUrl.port = if (xHttpUrl.scheme == "https") {
                    443
                } else {
                    80
                }
            }

            if (pos <= limit) {
                xHttpUrl.path = url.substring(pos, limit)
            }
            return this
        }

        fun build(): XHttpUrl {
            return xHttpUrl
        }
    }

    fun getNewUrl(): String {
        return "$scheme://$host:$port$path"
    }

    override fun toString(): String {
        return "XHttpUrl(scheme=$scheme, host='$host', port=$port, url=$url, authority=$authority, path=$path)"
    }


}
