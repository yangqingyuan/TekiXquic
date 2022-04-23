package com.lizhi.component.net.xquic.native

enum class XquicMsgType {

    INIT,

    /**
     * token
     */
    TOKEN,

    /**
     * session ticket
     */
    SESSION,

    /**
     * transport parameter
     */
    TP,

    /**
     * Headers
     */
    HEAD,

    /**
     * ping msg
     */
    PING,

    /**
     * native destroy
     */
    DESTROY
}