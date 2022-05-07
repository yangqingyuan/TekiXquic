package com.lizhi.component.net.xquic.mode

import org.json.JSONObject


/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XResponseBody(var data: String) {

    var body: String

    /**
     * 注意成功的时候返回tag，其他状态返回null
     */
    var tag: String? = null

    init {
        val jsonObject = JSONObject(data)
        body = jsonObject.getString("recv_body")
        if (jsonObject.has("tag")) {
            tag = jsonObject.getString("tag")
        }
    }
}