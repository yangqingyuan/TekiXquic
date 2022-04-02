package com.lizhi.component.net.xquic.listener

import com.lizhi.component.net.xquic.mode.XResponse
import java.lang.Exception

/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
interface XCallBack {
    fun onFailure(call: XCall, exception: Exception)
    fun onResponse(call: XCall, xResponse: XResponse)
}