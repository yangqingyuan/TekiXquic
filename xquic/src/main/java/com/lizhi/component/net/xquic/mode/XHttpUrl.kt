package com.lizhi.component.net.xquic.mode

import com.lizhi.component.net.xquic.listener.XDns
import com.lizhi.component.net.xquic.utils.IPUtils
import com.lizhi.component.net.xquic.utils.XLogUtils

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/6
 */
class XHttpUrl(val builder: Builder) {

    companion object {

        private const val TAG = "XHttpUrl"

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
    var scheme: String = builder.scheme


    /**
     * sample:https://192.168.10.21:8443/test?gws_rd=ssl
     * path = /test?gws_rd=ssl
     */
    var path: String? = builder.path

    /**
     * sample:https://192.168.10.21:8443/test?gws_rd=ssl
     * host = 192.168.10.21
     */
    var host: String? = builder.host
    var url: String = builder.url
    var port: Int = builder.port

    /**
     * sample:https://www.google.com.hk/?gws_rd=ssl
     * authority = www.google.com.hk
     */
    var authority: String = builder.authority

    class Builder {
        lateinit var scheme: String
        var path: String? = null
        var host: String? = null
        lateinit var url: String
        var port: Int = 0
        lateinit var authority: String

        fun parse(url: String) = apply {
            this.url = url
            var pos: Int = skipLeadingAsciiWhitespace(url, 0, url.length)
            val limit: Int = skipTrailingAsciiWhitespace(url, pos, url.length)

            // Scheme.
            val schemeDelimiterOffset: Int = schemeDelimiterOffset(url, pos, limit)

            when {
                url.regionMatches(0, "https:", 0, 6) -> {
                    this.scheme = "https"
                    pos += "https://".length
                }
                url.regionMatches(0, "http:", 0, 5) -> {
                    this.scheme = "http"
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
                this.authority = list[0]
                //if is ready ip
                if (IPUtils.isIpv4(this.authority) || IPUtils.isIpv6(this.authority)) {
                    this.host = list[0]
                }
            } else {
                throw IllegalArgumentException(
                    "Expected URL host url:$url"
                )
            }

            /* parse port */
            pos += this.authority.length + 1
            if (pos <= limit) {
                list = url.substring(pos, limit).split("/")
                if (list.isNotEmpty()) {
                    try {
                        this.port = list[0].toInt()
                        pos += list[0].length
                    } catch (e: Exception) {
                        pos -= 1
                    }
                }
            }

            if (this.port <= 0) {
                this.port = if (this.scheme == "https") {
                    443
                } else {
                    80
                }
            }

            if (pos <= limit) {
                this.path = url.substring(pos, limit)
            }
            return this
        }

        fun build(): XHttpUrl {
            return XHttpUrl(this)
        }
    }

    /**
     * use dns to get domain ip
     */
    fun getHostUrl(dns: XDns?): String? {
        if (!host.isNullOrEmpty()) {
            return "$scheme://$host:$port$path"
        }
        try {
            val address = dns?.lookup(authority)
            if (!address.isNullOrEmpty()) {
                return "$scheme://${address[0].hostAddress}:$port$path"//get first one
            }
        } catch (e: Exception) {
            XLogUtils.error(TAG, e)
        }
        return "$scheme://$authority:$port$path"
    }

    override fun toString(): String {
        return "XHttpUrl(scheme=$scheme, host='$host', port=$port, url=$url, authority=$authority, path=$path)"
    }

    fun newUrl(builder: Builder): XHttpUrl {
        return XHttpUrl(builder)
    }


}
