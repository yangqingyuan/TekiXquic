package com.lizhi.component.net.xquic.mode

import androidx.fragment.app.FragmentActivity

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRequest {

    lateinit var url: XHttpUrl
    var body: XRequestBody? = null

    private var key = System.currentTimeMillis().toString()
    private val tags by lazy { mutableMapOf<String, String>() }

    var method: String = "GET" // or POST
    var headers: XHeaders.Builder = XHeaders.Builder()


    fun tag(): String? {
        return tags[key]
    }

    /**
     * to observer activity life
     */
    var life: FragmentActivity? = null

    class Builder {
        private val xRequest = XRequest()

        fun build(): XRequest {
            return xRequest
        }

        fun url(url: String): Builder {
            xRequest.url = XHttpUrl.get(url)
            return this
        }


        fun header(name: String, value: String): Builder {
            xRequest.headers.set(name, value)
            return this
        }

        fun addHeader(name: String, value: String): Builder {
            xRequest.headers.add(name, value)
            return this
        }


        fun removeHeader(name: String, value: String): Builder {
            xRequest.headers.set(name, value)
            return this
        }

        fun tag(tag: String): Builder {
            xRequest.tags[xRequest.key] = tag
            return this
        }

        fun life(activity: FragmentActivity): Builder {
            xRequest.life = activity
            return this
        }

        fun headers(heards: XHeaders): Builder {
            xRequest.headers = heards.newBuilder()
            return this
        }

        fun get(): Builder {
            xRequest.method = "GET"
            return this
        }

        fun get(xRequestBody: XRequestBody): Builder {
            xRequest.method = "GET"
            xRequest.body = xRequestBody
            return this
        }

        fun post(): Builder {
            xRequest.method = "POST"
            return this
        }

        fun body(xRequestBody: XRequestBody): Builder {
            xRequest.body = xRequestBody
            return this
        }

        fun post(xRequestBody: XRequestBody): Builder {
            xRequest.method = "POST"
            xRequest.body = xRequestBody
            return this
        }
    }
}