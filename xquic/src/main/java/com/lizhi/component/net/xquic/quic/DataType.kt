package com.lizhi.component.net.xquic.quic

import androidx.annotation.IntDef
import com.lizhi.component.net.xquic.mode.XMediaType

/**
 * send to native data type
 */
class DataType {
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @IntDef(OTHER, JSON, BYTE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation
    class Type

    companion object {
        const val OTHER = -1
        const val JSON = 0 //json
        const val BYTE = 1 //byte

        fun getDataTypeByMediaType(mediaType: XMediaType): Int {
            if (mediaType.mediaType == XMediaType.MEDIA_TYPE_MULTIPART) {
                return BYTE
            }
            return JSON
        }

    }
}