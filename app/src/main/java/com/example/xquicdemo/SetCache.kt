package com.example.xquicdemo

import android.content.Context
import com.lizhi.component.net.xquic.native.CCType

object SetCache {

    private const val key = "tekixuqic"

    private const val KEY_CC_TYPE = "cc_type"
    private const val KEY_METHOD = "method"
    private const val KEY_URL = "url"
    private const val KEY_CONN_TIMEOUT = "conn_time_out"
    private const val KEY_TEST_COUNT = "conn_test_count"


    fun setCCType(context: Context, ccType: CCType) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_CC_TYPE, ccType.ordinal).apply()
    }

    fun getCCType(context: Context): CCType {
        return when (context.getSharedPreferences(key, 0)
            .getInt(KEY_CC_TYPE, CCType.BBR.ordinal)) {
            CCType.BBR.ordinal -> {
                CCType.BBR
            }
            CCType.CUBIC.ordinal -> {
                CCType.CUBIC
            }
            else -> {
                CCType.RENO
            }
        }
    }


    fun setMethod(context: Context, getOrPost: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_METHOD, getOrPost).apply()
    }

    fun getMethod(context: Context): String {
        return context.getSharedPreferences(key, 0).getString(KEY_METHOD, "GET")!!
    }

    fun setUrl(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL, url).apply()
    }

    fun getUrl(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL, "https://192.168.8.120:8443")
    }

    fun setConnTimeOut(context: Context, timeout: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_CONN_TIMEOUT, timeout).apply()
    }

    fun getConnTimeout(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_CONN_TIMEOUT, 10)
    }


    fun setTestCount(context: Context, count: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_TEST_COUNT, count).apply()
    }

    fun getTestCount(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_TEST_COUNT, 1)
    }
}