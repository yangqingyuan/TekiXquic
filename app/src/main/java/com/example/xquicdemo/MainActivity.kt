package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private fun getData(): String {
        return SimpleDateFormat("dd hh:mm:ss").format(Date())
    }

    fun appendText(context: String?) {

        textView?.let {

            runOnUiThread {
                it.append(getData() + " : " + context)
                val scrollAmount = it.layout?.getLineTop(it.lineCount)!! - it.height
                if (scrollAmount > 0) {
                    it.scrollTo(0, scrollAmount)
                } else {
                    it.scrollTo(0, 0)
                }
            }
        }

    }

    var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.tv_result)

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {

            val xquicClient = XquicClient.Builder()
                .connectTimeOut(13)
                .setReadTimeOut(23)
                .writeTimeout(15)
                .pingInterval(15)
                .build()

            val xRequest = XRequest.Builder()
                .url("https://192.168.10.245:8442")
                .get() //Default
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

    }
}