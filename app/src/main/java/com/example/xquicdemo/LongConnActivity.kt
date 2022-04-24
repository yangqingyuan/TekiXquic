package com.example.xquicdemo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XWebSocket
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.text.SimpleDateFormat
import java.util.*


class LongConnActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var etContent: EditText

    private lateinit var webSocket: XWebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_long)
        title = "Long Conn"

        textView = findViewById(R.id.tv_result)
        etContent = findViewById(R.id.et_content)

        initWebSocket()

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {
            val testCount = SetCache.getTestCount(applicationContext)
            for (i in (1..testCount)) {
                webSocket.send(etContent.text.toString() + ",index=" + i)
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
            webSocket.cancel()
        }
    }

    private fun initWebSocket() {
        val xquicClient = XquicClient.Builder()
            .connectTimeOut(SetCache.getConnTimeout(applicationContext))
            .ccType(SetCache.getCCType(applicationContext))
            .setReadTimeOut(23) //TODO 未实现
            .writeTimeout(15)//TODO 未实现
            .pingInterval(5000)//
            .build()

        val url = SetCache.getSelectUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }

        val xRequest = XRequest.Builder()
            .url(url)//127.0.0.1:6121 //192.168.10.245:8443
            .get() //Default
            .addHeader("testA", "testA")
            .build()

        webSocket = xquicClient.newWebSocket(xRequest, object : XWebSocketListener {
            override fun onOpen(webSocket: XWebSocket, response: XResponse) {
                XLogUtils.error("onOpen")
            }

            override fun onMessage(webSocket: XWebSocket, data: ByteArray) {
                parseResponse(data)
            }

            override fun onFailure(
                webSocket: XWebSocket,
                exception: Throwable,
                response: XResponse
            ) {
                exception.printStackTrace()
                XLogUtils.error(exception.message)
                appendText("${exception.message}")
            }
        })

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

    private fun parseResponse(data: ByteArray) {
        var content = String(data)
        if (content.length > 512 * 1024) {
            content = "数据太大，无法打印和显示，数据长度为:" + content.length
        }

        XLogUtils.error(
            " java ize=${content.length},content=${content}"
        )

        appendText(content)
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.cancel()
    }
}