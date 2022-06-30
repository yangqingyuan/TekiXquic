package com.lizhi.component.net.xquic.mode

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference


/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XRequest(val builder: Builder) {

    private val key = builder.key
    var url: XHttpUrl = builder.url
    var body: XRequestBody? = builder.body

    private val tags = builder.tags

    var method: String = builder.method
    var headers: XHeaders = builder.headers

    var life: WeakReference<FragmentActivity>? = builder.life

    fun tag(): List<String> {
        val list = mutableListOf<String>()
        tags.forEach {
            list.add(it.value)
        }
        return list
    }

    fun tag(key: String): String? {
        return tags[key]
    }

    fun userTag(): String? {
        return tags[key]
    }

    /**
     * copy and new data
     */
    fun newRequest(): XRequest {
        return XRequest(builder)
    }

    class Builder {

        lateinit var url: XHttpUrl
        internal var body: XRequestBody? = null

        internal var key = System.currentTimeMillis().toString()
        internal val tags = mutableMapOf<String, String>()

        var method: String = "GET" // or POST
        var headers: XHeaders = XHeaders.Builder().build()

        var life: WeakReference<FragmentActivity>? = null

        fun build(): XRequest {
            return XRequest(this)
        }

        fun url(url: String) = apply {
            this.url = XHttpUrl.get(url)
        }


        fun header(name: String, value: String) = apply {
            this.headers.headersMap[name] = value
        }

        fun addHeader(name: String, value: String) = apply {
            this.headers.headersMap[name] = value
        }

        fun removeHeader(name: String) = apply {
            this.headers.headersMap.remove(name)
        }

        fun tag(tag: String) = apply {
            this.tags[key] = tag
        }

        fun tag(key: String, tag: String) = apply {
            this.tags[key] = tag
        }

        fun life(activity: FragmentActivity) = apply {
            this.life = WeakReference(activity)
        }

        fun headers(header: XHeaders) = apply {
            this.headers = header
        }

        fun get() = apply {
            this.method = "GET"
        }

        fun get(xRequestBody: XRequestBody) = apply {
            this.method = "GET"
            this.body = xRequestBody
        }

        fun post() = apply {
            this.method = "POST"
        }

        fun body(xRequestBody: XRequestBody) = apply {
            this.body = xRequestBody
        }

        fun post(xRequestBody: XRequestBody) = apply {
            this.method = "POST"
            this.body = xRequestBody
        }
    }
}