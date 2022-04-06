package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XMediaType
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XRequestBody
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.native.CCType
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    var textView: TextView? = null
    var radioGroup: RadioGroup? = null
    var etContent: EditText? = null

    private val xquicClient = XquicClient.Builder()
        .connectTimeOut(13)
        .setReadTimeOut(23)
        .writeTimeout(15)
        .pingInterval(15)
        .ccType(CCType.BBR)
        .authority("test_authority")
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.tv_result)

        radioGroup = findViewById(R.id.radioGroup)

        etContent = findViewById(R.id.et_content)

        radioGroup?.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.btn_bbr -> {
                    xquicClient.ccType = CCType.BBR
                    XLogUtils.info("BBR")
                }
                R.id.btn_cubic -> {
                    xquicClient.ccType = CCType.CUBIC
                    XLogUtils.info("CUBIC")
                }
                else -> {
                    xquicClient.ccType = CCType.RENO
                    XLogUtils.info("ccType")
                }
            }
        }


        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {
            //get()
            post()
        }

    }


    private fun getData(): String {
        return SimpleDateFormat("dd hh:mm:ss").format(Date())
    }

    fun appendText(context: String?) {

        textView?.let {

            runOnUiThread {
                it.append(getData() + " : " + context + "\n")
                val scrollAmount = it.layout?.getLineTop(it.lineCount)!! - it.height
                if (scrollAmount > 0) {
                    it.scrollTo(0, scrollAmount)
                } else {
                    it.scrollTo(0, 0)
                }
            }
        }

    }


    private fun get() {

        val xRequest = XRequest.Builder()
            .url("https://192.168.10.245:8443/demo/tile")//127.0.0.1:6121 //192.168.10.245:8443
            .get() //Default
            .addHeader("testA", "testA")
            .build()

        val startTime = System.currentTimeMillis()
        xquicClient.newCall(xRequest).enqueue(object : XCallBack {
            override fun onFailure(call: XCall, exception: Exception) {
                exception.printStackTrace()
                XLogUtils.error(exception.message)
            }

            override fun onResponse(call: XCall, xResponse: XResponse) {

                XLogUtils.error(
                    " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody?.getData()}"
                )

                appendText(xResponse.xResponseBody?.getData())
            }
        })
    }

    private fun post() {
        val content = etContent?.text
        val xRequestBody =
            XRequestBody.create(XMediaType.parse(XMediaType.MEDIA_TYPE_TEXT), content.toString())
        val xRequest = XRequest.Builder()
            .url("https://192.168.10.245:8443/demo/tile")//127.0.0.1:6121 //192.168.10.245:8443
            .post(xRequestBody) //Default
            .build()

        XLogUtils.info("start post content=$content")
        val startTime = System.currentTimeMillis()
        xquicClient.newCall(xRequest).enqueue(object : XCallBack {
            override fun onFailure(call: XCall, exception: Exception) {
                exception.printStackTrace()
                XLogUtils.error(exception.message)
            }

            override fun onResponse(call: XCall, xResponse: XResponse) {

                XLogUtils.error(
                    " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody?.getData()}"
                )

                appendText(xResponse.xResponseBody?.getData())
            }
        })
    }
}