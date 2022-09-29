package com.lizhi.component.net.xquic.native

import androidx.annotation.IntDef

/**
 * 是否加密
 */
class CryptoFlag {
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @IntDef(CRYPTO, WITHOUT_CRYPTO)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation
    class Type

    companion object {
        const val CRYPTO = 0 //加密
        const val WITHOUT_CRYPTO = 1 //不加密
    }
}