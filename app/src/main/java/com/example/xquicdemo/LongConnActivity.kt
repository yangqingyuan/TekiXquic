package com.example.xquicdemo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XPingListener
import com.lizhi.component.net.xquic.listener.XWebSocket
import com.lizhi.component.net.xquic.listener.XWebSocketListener
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.utils.XLogUtils
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*


class LongConnActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var etContent: EditText

    private lateinit var webSocket: XWebSocket

    private var launch: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_long)
        title = "Long Conn"

        textView = findViewById(R.id.tv_result)
        etContent = findViewById(R.id.et_content)

        initWebSocket()

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {
            val testCount = SetCache.getTestCount(applicationContext)
            val timeSpace = SetCache.getTestSpace(applicationContext)
            launch = CoroutineScope(Dispatchers.Default).launch {
                for (i in (1..testCount)) {
                    webSocket.send(
                        etContent.text.toString() + ",index=" + i,
                        System.currentTimeMillis().toString() //这里使用当前时间来做tag发送，是为了收到消息的时候计算耗时
                    )
                    if (timeSpace > 0) {
                        delay(timeSpace * 1000L)
                    }
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
            webSocket.cancel()
            //webSocket.close(11, "sdsdf")
        }
    }

    private fun initWebSocket() {
        val xquicClient = XquicClient.Builder()
            .connectTimeOut(SetCache.getConnTimeout(applicationContext))
            .ccType(SetCache.getCCType(applicationContext))
            .pingInterval(5000)//
            //.dns(XDns.SYSTEM)
            .addPingListener(object : XPingListener {
                //可选
                override fun ping(): String {
                    return "ping data"
                }

                override fun pong(data: String) {
                    //XLogUtils.info("data=$data")
                }

            })
            .build()

        val url = SetCache.getSelectUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }

        val methodGet = SetCache.getMethod(applicationContext) == "GET"

        val xRequest = if (methodGet) {
            XRequest.Builder()
                .url(url)
                .get() //Default
                .addHeader("testA", "testA")
                .build()
        } else {
            XRequest.Builder()
                .url(url)
                .post()
                .addHeader("testA", "testA")
                .build()
        }

        webSocket = xquicClient.newWebSocket(xRequest, object : XWebSocketListener {
            override fun onOpen(webSocket: XWebSocket, response: XResponse) {
                XLogUtils.error("onOpen")
            }

            override fun onMessage(webSocket: XWebSocket, response: XResponse) {
                parseResponse(response)
            }

            override fun onClosed(webSocket: XWebSocket, code: Int, reason: String?) {
                launch?.cancel()
                appendText("closed: code=${code},readson=${reason}")
            }

            override fun onFailure(
                webSocket: XWebSocket,
                exception: Throwable,
                response: XResponse
            ) {
                launch?.cancel()
                exception.printStackTrace()
                XLogUtils.error(exception.message)
                appendText("${exception.message}")
            }
        })

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

    private fun parseResponse(response: XResponse) {
        var content = response.xResponseBody.body
        val tag = response.xResponseBody.tag

        var costTime = 0L
        tag?.let {
            costTime = System.currentTimeMillis() - tag.toLong()
        }
        if (content.length > 512 * 1024) {
            content = "数据太大，无法打印和显示，数据长度为:" + content.length
        }

        XLogUtils.error(
            " java ize=${content.length},content=${content}"
        )

        appendText(
            "$content ,time=${costTime} ms, status=" + response.getStatus()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.cancel()
        launch?.cancel()
    }
}