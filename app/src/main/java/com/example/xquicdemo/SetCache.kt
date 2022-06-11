package com.example.xquicdemo

import android.content.Context
import com.lizhi.component.net.xquic.native.CCType

object SetCache {

    private const val key = "tekixuqic"

    private const val KEY_CC_TYPE = "cc_type"
    private const val KEY_METHOD = "method"
    private const val KEY_URL = "url"
    private const val KEY_URL1 = "url1"
    private const val KEY_URL2 = "url2"
    private const val KEY_SELECT = "select"
    private const val KEY_REUSE = "reuse"
    private const val KEY_CONN_TIMEOUT = "conn_time_out"
    private const val KEY_TEST_COUNT = "conn_test_count"
    private const val KEY_TEST_SPACE = "conn_test_space"


    fun setCCType(context: Context, ccType: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_CC_TYPE, ccType).apply()
    }

    fun getCCType(context: Context): Int {
        return when (context.getSharedPreferences(key, 0)
            .getInt(KEY_CC_TYPE, CCType.BBR)) {
            CCType.BBR -> {
                CCType.BBR
            }
            CCType.CUBIC -> {
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

    fun getSelectUrl(context: Context): String? {
        when (getSelect(context)) {
            0 -> {
                return getUrl(context)
            }
            1 -> {
                return getUrl1(context)
            }
            2 -> {
                return getUrl2(context)
            }
        }
        return null
    }


    fun getSelect(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_SELECT, 0)
    }

    fun setSelect(context: Context, index: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_SELECT, index).apply()
    }

    fun getReuse(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_REUSE, 0)
    }

    fun setReuse(context: Context, index: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_REUSE, index).apply()
    }


    fun setUrl(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL, url).apply()
    }

    fun getUrl(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL, "https://192.168.8.120:8443")
    }


    fun setUrl1(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL1, url).apply()
    }

    fun getUrl1(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL1, "")
    }


    fun setUrl2(context: Context, url: String) {
        context.getSharedPreferences(key, 0).edit().putString(KEY_URL2, url).apply()
    }

    fun getUrl2(context: Context): String? {
        return context.getSharedPreferences(key, 0).getString(KEY_URL2, "")
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

    fun getTestSpace(context: Context): Int {
        return context.getSharedPreferences(key, 0).getInt(KEY_TEST_SPACE, 0)
    }

    fun setTestSpace(context: Context, count: Int) {
        context.getSharedPreferences(key, 0).edit().putInt(KEY_TEST_SPACE, count).apply()
    }
}