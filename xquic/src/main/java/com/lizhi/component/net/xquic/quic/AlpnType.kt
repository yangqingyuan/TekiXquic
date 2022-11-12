package com.lizhi.component.net.xquic.quic

import androidx.annotation.IntDef

/**
 * 协议类型（应用层协议协商（Application-Layer Protocol Negotiation，简称ALPN））
 */
class AlpnType {

    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @IntDef(ALPN_HQ, ALPN_H3)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Type

    companion object {
        const val ALPN_HQ = 0
        const val ALPN_H3 = 1
    }
}