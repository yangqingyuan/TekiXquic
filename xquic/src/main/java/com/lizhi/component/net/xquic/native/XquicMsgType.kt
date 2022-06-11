package com.lizhi.component.net.xquic.native

object XquicMsgType {

    const val INIT = 0

    /**
     * hand_shake
     */
    const val HANDSHAKE = 1

    /**
     * token
     */
    const val TOKEN = 2

    /**
     * session ticket
     */
    const val SESSION = 3

    /**
     * transport parameter
     */
    const val TP = 4

    /**
     * Headers
     */
    const val HEAD = 5

    /**
     * ping msg
     */
    const val PING = 6

    /**
     * native destroy
     */
    const val DESTROY = 7
}