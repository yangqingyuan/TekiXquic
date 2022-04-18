package com.lizhi.component.net.xquic.native

import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception


/**
 * 作用:
 * 作者: yqy
 * 创建日期: 2022/4/18.
 */
object XquicLoader {
    var libLoaded: Boolean = false
    fun loadLib() {
        try {
            synchronized(this) {
                if (!libLoaded) {
                    System.loadLibrary("xnet-lib")
                    System.loadLibrary("xquic")
                    libLoaded = true
                }
            }
        } catch (e: Exception) {
            XLogUtils.error(e)
        }
    }
}