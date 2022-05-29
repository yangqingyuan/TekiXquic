package com.lizhi.component.net.xquic.native

class SendBody {
    data class Header(
        var name: String,
        var value: String,
        var flags: Int = 0
    )

    var send_body: String? = null
    var user_tag: String? = null
    var headers: MutableList<Header> = mutableListOf()
}