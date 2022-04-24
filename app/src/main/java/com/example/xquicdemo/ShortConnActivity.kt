package com.example.xquicdemo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XMediaType
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XRequestBody
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*


class ShortConnActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var etContent: EditText

    private lateinit var xquicClient: XquicClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_short)
        title = "Short Conn"

        xquicClient = XquicClient.Builder()
            .connectTimeOut(SetCache.getConnTimeout(applicationContext))
            .ccType(SetCache.getCCType(applicationContext))
            .setReadTimeOut(23) //TODO 未实现
            .writeTimeout(15)//TODO 未实现
            .build()

        textView = findViewById(R.id.tv_result)

        etContent = findViewById(R.id.et_content)

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {
            val testCount = SetCache.getTestCount(applicationContext)
            val methodGet = SetCache.getMethod(applicationContext) == "GET"
            for (i in (1..testCount)) {
                if (methodGet) {
                    get(i)
                } else {
                    post(i)
                }
            }
        }

        findViewById<Button>(R.id.btn_set).setOnClickListener {
            val intent = Intent(this, SetActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_clean).setOnClickListener {
            textView.text = "返回结果：\n"
        }

    }

    override fun onResume() {
        super.onResume()
        val methodGet = SetCache.getMethod(applicationContext) == "GET"
        if (methodGet) {
            etContent.visibility = View.GONE
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getData(): String {
        return SimpleDateFormat("dd hh:mm:ss").format(Date())
    }

    private fun appendText(context: String?) {
        textView.let {
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


    private fun get(index: Int) {
        val url = SetCache.getUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }
        val xRequest = XRequest.Builder()
            .url(url)
            .get() //Default
            .addHeader("testA", "testA")
            .tag("tag")
            .life(this)//可选，如果传递这个参数，内部可以根据activity的生命周期取消没有执行的任务或者正在执行的任务，例如超时
            .build()
        request(index, xRequest)
    }

    private fun post(index: Int) {

        val url = SetCache.getSelectUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }

        val content = etContent.text
        val xRequestBody =
            XRequestBody.create(XMediaType.parse(XMediaType.MEDIA_TYPE_TEXT), content.toString())
        val xRequest = XRequest.Builder()
            .url(url)
            .post(xRequestBody) //Default
            .tag("tag")
            .life(this)//可选，如果传递这个参数，内部可以根据activity的生命周期取消没有执行的任务或者正在执行的任务，例如超时
            .build()
        request(index, xRequest)
    }


    private fun request(index: Int, xRequest: XRequest) {
        if (index == 1) {
            val requestInfo = StringBuilder()
            requestInfo.append("拥塞算法：" + SetCache.getCCType(applicationContext) + "\n")
            requestInfo.append("链接超时：" + SetCache.getConnTimeout(applicationContext) + " 秒\n")
            requestInfo.append("请求方式：" + SetCache.getMethod(applicationContext) + "\n")
            requestInfo.append("轮询次数：" + SetCache.getTestCount(applicationContext) + " 次\n")
            requestInfo.append("请求url：" + xRequest.url.url + "\n")
            appendText(requestInfo.toString())
        }


        val startTime = System.currentTimeMillis()
        xquicClient.newCall(xRequest).enqueue(object : XCallBack {
            override fun onFailure(call: XCall, exception: Exception) {
                exception.printStackTrace()
                XLogUtils.error(exception.message)
                appendText("${exception.message}")
            }

            override fun onResponse(call: XCall, xResponse: XResponse) {
                parseResponse(startTime, index, xResponse)
            }
        })
    }

    private fun parseResponse(startTime: Long, index: Int, xResponse: XResponse) {
        var content: String = xResponse.xResponseBody.getData()
        if (content.length > 512 * 1024) {
            content = "数据太大，无法打印和显示，数据长度为:" + content.length
        }

        val now = System.currentTimeMillis()

        XLogUtils.error(
            " java 总花费时长： ${(now - startTime)} ms,队列等待时长：${xResponse.delayTime} ms,请求响应时长：${now - startTime - xResponse.delayTime} ms,size=${content.length},content=${content}"
        )

        appendText("$content ,index=$index")
    }

}