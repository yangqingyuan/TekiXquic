package com.example.xquicdemo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.impl.XAsyncCall
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.mode.XMediaType
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XRequestBody
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.native.SendParams
import com.lizhi.component.net.xquic.native.XquicCallback
import com.lizhi.component.net.xquic.native.XquicLongNative
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*


class LongConnActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var etContent: EditText

    private lateinit var xquicLongNative: XquicLongNative

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_long)
        title = "Long Conn"

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

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            xquicLongNative.cancel(xquicLongNative.clientCtx)
        }

        findViewById<Button>(R.id.btn_ping).setOnClickListener {
            xquicLongNative.sendPing(xquicLongNative.clientCtx, "ping")
        }


        xquicLongNative = XquicLongNative()

        val head = hashMapOf<String, String>()
        head.put("test", "test")
        val sendParamsBuilder = SendParams.Builder()
            .setUrl(SetCache.getUrl(applicationContext)!!)
            //.setToken(XAsyncCall.tokenMap[url()])
            //.setSession(XAsyncCall.sessionMap[url()])
            .setTimeOut(SetCache.getConnTimeout(applicationContext))
            .setMaxRecvLenght(1024 * 1024)
            .setHeaders(head)
        //.setCCType(xquicClient.ccType)

        Thread {
            xquicLongNative.clientCtx =
                xquicLongNative.connect(sendParamsBuilder.build(), object : XquicCallback {
                    override fun callBackReadData(ret: Int, data: ByteArray) {
                        XLogUtils.error("callBackReadData ret=$ret, content=${String(data)}")
                    }

                    override fun callBackMessage(msgType: Int, data: ByteArray) {
                        XLogUtils.error("callBackMessage msgType=$msgType, content=${String(data)}")
                    }
                })

            xquicLongNative.start(xquicLongNative.clientCtx)

            XLogUtils.error("java 结束")
        }.start()
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
            .url(url)//127.0.0.1:6121 //192.168.10.245:8443
            .get() //Default
            .addHeader("testA", "testA")
            .build()
        request(index, xRequest)
    }

    private fun post(index: Int) {

        val url = SetCache.getUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }

        val content = etContent.text
        val xRequestBody =
            XRequestBody.create(XMediaType.parse(XMediaType.MEDIA_TYPE_TEXT), content.toString())
        val xRequest = XRequest.Builder()
            .url(url)//127.0.0.1:6121 //192.168.10.245:8443
            .post(xRequestBody) //Default
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

        xquicLongNative.send(xquicLongNative.clientCtx, "atb")
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