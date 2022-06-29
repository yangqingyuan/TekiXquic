package com.lizhi.component.net.xquic.mode

import com.lizhi.component.net.xquic.utils.XLogUtils
import org.json.JSONObject


/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponseBody(var data: ByteArray) {

    var body: String

    /**
     * 注意成功的时候返回tag，其他状态返回null
     */
    var tag: String? = null

    init {
        try {
            val jsonObject = JSONObject(String(data))
            body = if (jsonObject.has("recv_body")) {
                jsonObject.getString("recv_body")
            } else {
                ""
            }
            if (jsonObject.has("tag")) {
                tag = jsonObject.getString("tag")
            }
        } catch (e: Exception) {
            XLogUtils.error(e)
            body = ""
        }
    }
}