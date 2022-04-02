package com.lizhi.component.net.xquic.utils

import android.util.Log

object XLogUtils {

    const val commonTag = "LzXquic"

    /**
     * @param tag:
     * @param message: log 信息
     */
    fun verbose(tag: String, message: String?) {
        Log.v("$commonTag:$tag", message ?: "")
    }

    fun verbose(message: String?) {
        Log.v("$commonTag", message ?: "")
    }

    /**
     * @param tag:
     * @param message: log 信息
     */
    fun debug(tag: String, message: String?) {
        Log.d("$commonTag:$tag", message ?: "")
    }

    fun debug(message: String?) {
        Log.d("$commonTag", message ?: "")
    }


    /**
     * @param tag:
     * @param message: log 信息
     */
    fun info(tag: String, message: String?) {
        Log.i("$commonTag:$tag", message ?: "")
    }

    fun info(message: String?) {
        Log.i("$commonTag", message ?: "")
    }

    /**
     * @param tag:
     * @param message: log 信息
     */
    fun warn(tag: String, message: String?) {
        Log.w("$commonTag:$tag", message ?: "")
    }

    fun warn(message: String?) {
        Log.w("$commonTag:", message ?: "")
    }

    /**
     * @param tag:
     * @param message: log 信息
     */
    fun error(tag: String? = "", message: String?) {
        Log.e("$commonTag:$tag", message ?: "")
    }

    fun error(message: String?) {
        Log.e("$commonTag", message ?: "")
    }

    /**
     * @param tag:
     * @param throwable: exception对象
     */
    fun error(tag: String? = "", throwable: Throwable?) {
        Log.e("$commonTag:$tag", throwable?.message ?: "")
    }

    fun error(throwable: Throwable?) {
        Log.e("$commonTag:", throwable?.message ?: "")

    }
}