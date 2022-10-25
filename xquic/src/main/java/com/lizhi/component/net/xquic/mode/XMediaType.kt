package com.lizhi.component.net.xquic.mode

import androidx.annotation.StringDef

class XMediaType(@XMediaType.Type val mediaType: String) {
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @StringDef(
        MEDIA_TYPE_TEXT,
        MEDIA_TYPE_FORM,
        MEDIA_TYPE_FORM_UTF8,
        MEDIA_TYPE_JSON,
        MEDIA_TYPE_JSON_UTF8,
        MEDIA_TYPE_MULTIPART
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation
    class Type

    companion object {

        const val MEDIA_TYPE_TEXT = "text/plain"
        const val MEDIA_TYPE_FORM = "application/x-www-form-urlencoded"
        const val MEDIA_TYPE_FORM_UTF8 = "application/x-www-form-urlencoded;charset=UTF-8"
        const val MEDIA_TYPE_JSON = "application/json"
        const val MEDIA_TYPE_JSON_UTF8 = "application/json;charset=UTF-8"
        const val MEDIA_TYPE_MULTIPART = "multipart/form-data"

        fun parse(@XMediaType.Type mediaType: String): XMediaType {
            return XMediaType(mediaType)
        }
    }
}