package com.lizhi.component.net.xquic.quic

import androidx.annotation.IntDef

/**
 * 作用: 拥塞算法
 * 作者: yqy
 * 创建日期: 2022/4/2.
 */

class CCType {
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @IntDef(BBR, CUBIC, RENO)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Type

    companion object {
        const val BBR = 0
        const val CUBIC = 1
        const val RENO = 2
    }
}