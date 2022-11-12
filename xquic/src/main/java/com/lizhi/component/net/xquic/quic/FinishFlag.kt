package com.lizhi.component.net.xquic.quic

import androidx.annotation.IntDef

/**
 * request finish flag, 1 for finish.
 */
class FinishFlag {

    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @IntDef(FINISH, WITHOUT_FINISH)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Type

    companion object {
        const val WITHOUT_FINISH = 0 //不关闭
        const val FINISH = 1 //关闭
    }
}