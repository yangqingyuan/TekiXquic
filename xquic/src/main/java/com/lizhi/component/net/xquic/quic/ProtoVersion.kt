package com.lizhi.component.net.xquic.quic

import androidx.annotation.IntDef

/**
 * 协议版本号
 * QUIC protocol version
 */
class ProtoVersion {

    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @MustBeDocumented
    @IntDef(
        XQC_IDRAFT_INIT_VER,
        XQC_VERSION_V1,
        XQC_IDRAFT_VER_29,
        XQC_IDRAFT_VER_NEGOTIATION,
        XQC_VERSION_MAX
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation
    class Version

    companion object {
        /* placeholder */
        const val XQC_IDRAFT_INIT_VER = 0

        /* former version of QUIC RFC 9000 */
        const val XQC_VERSION_V1 = 1

        /* IETF Draft-29 */
        const val XQC_IDRAFT_VER_29 = 2

        /* Special version for version negotiation. */
        const val XQC_IDRAFT_VER_NEGOTIATION = 3

        /* Support version count. */
        const val XQC_VERSION_MAX = 4
    }
}