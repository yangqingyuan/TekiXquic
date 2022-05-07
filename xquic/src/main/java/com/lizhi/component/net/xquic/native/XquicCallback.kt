package com.lizhi.component.net.xquic.native

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
interface XquicCallback {
    companion object {
        const val XQC_OK = 0
        const val XQC_ERROR = -1
    }

    /**
     * 读取后端返回的数据
     */
    fun callBackData(ret: Int, data: String)


    /**
     * token回调
     * 0:token
     * 1:session
     * 2:tp
     */
    fun callBackMessage(msgType: Int, data: String)
}