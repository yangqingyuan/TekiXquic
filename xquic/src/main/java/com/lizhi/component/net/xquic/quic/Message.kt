package com.lizhi.component.net.xquic.quic

import com.google.gson.Gson
import com.lizhi.component.net.xquic.mode.XRequestBody
import java.util.HashMap

/**
 * message Internal use
 */
class Message {
    companion object {
        private const val DATA_TYPE_OTHER = DataType.OTHER
        private const val DATA_TYPE_JSON = DataType.JSON
        private const val DATA_TYPE_BYTE = DataType.BYTE

        const val MSG_TYPE_SEND = 0
        const val MSG_TYPE_CLOSE = 1
        private val gson = Gson()

        fun makeMessageByReqBody(
            dataType: Int,
            xRequestBody: XRequestBody
        ): Message {
            xRequestBody.let {
                val message: Message = if (dataType == DataType.JSON) {
                    val data = if (it.content !is String) {
                        String(it.content as ByteArray)
                    } else {
                        it.content as String
                    }
                    makeJsonMessage(data, "", null)
                } else {
                    val data = if (it.content is String) {
                        (it.content as String).toByteArray()
                    } else {
                        it.content as ByteArray
                    }
                    makeByteMessage(data)
                }
                return message
            }
        }


        fun makeJsonMessage(
            msgContent: String, tag: String? = null, header: HashMap<String, String>? = null
        ): Message {
            val message = Message()
            message.dataType = DATA_TYPE_JSON
            message.msgType = MSG_TYPE_SEND
            val sendBody = SendBody()
            sendBody.send_body = msgContent
            sendBody.user_tag = tag
            header?.forEach(action = {
                sendBody.headers.add(SendBody.Header(it.key, it.value))
            })
            message.byteArray = gson.toJson(sendBody).toByteArray()
            return message
        }

        fun makeByteMessage(
            byteArray: ByteArray
        ): Message {
            val message = Message()
            message.dataType = DATA_TYPE_BYTE
            message.msgType = MSG_TYPE_SEND
            message.byteArray = byteArray
            return message
        }

        fun makeCloseMessage(): Message {
            val message = Message()
            message.dataType = DATA_TYPE_OTHER
            message.msgType = MSG_TYPE_CLOSE
            return message
        }
    }

    var dataType: Int = DATA_TYPE_JSON
    var msgType: Int = MSG_TYPE_SEND
    lateinit var byteArray: ByteArray

    fun getContent(): ByteArray {
        return byteArray
    }

    fun getContentLength(): Int {
        return byteArray.size
    }
}