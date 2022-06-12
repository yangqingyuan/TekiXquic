package com.lizhi.component.net.xquic.mode

class XMediaType(val mediaType: String) {

    companion object {

        const val MEDIA_TYPE_TEXT = "text/plain"
        const val MEDIA_TYPE_FORM = "application/x-www-form-urlencoded"
        const val MEDIA_TYPE_FORM_UTF8 = "application/x-www-form-urlencoded;charset=UTF-8"
        const val MEDIA_TYPE_JSON = "application/json"
        const val MEDIA_TYPE_JSON_UTF8 = "application/json;charset=UTF-8"
        const val MEDIA_TYPE_MULTIPART = "multipart/form-data"
    }
}