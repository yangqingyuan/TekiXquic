package com.example.xquicdemo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.lizhi.component.net.xquic.XquicClient
import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XCallBack
import com.lizhi.component.net.xquic.listener.XDns
import com.lizhi.component.net.xquic.mode.XMediaType
import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.mode.XRequestBody
import com.lizhi.component.net.xquic.mode.XResponse
import com.lizhi.component.net.xquic.quic.AlpnType
import com.lizhi.component.net.xquic.quic.CryptoFlag
import com.lizhi.component.net.xquic.quic.ProtoVersion
import com.lizhi.component.net.xquic.utils.XLogUtils
import kotlinx.coroutines.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Exception


class ShortConnActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var etContent: EditText

    private lateinit var xquicClient: XquicClient

    private var launch: Job? = null

    private var successCount = 0
    private var failCount = 0
    private var startTime = System.currentTimeMillis()
    private val atomicInteger = AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_short)
        title = "Short Conn"

        xquicClient = XquicClient.Builder()
            .connectTimeOut(SetCache.getConnTimeout(applicationContext))
            .ccType(SetCache.getCCType(applicationContext))
            .setReadTimeOut(SetCache.getConnTimeout(applicationContext))
            .writeTimeout(15)//TODO 未实现
            //.setFinishFlag(FinishFlag.FINISH)//
            //.setCryptoFlag(CryptoFlag.WITHOUT_CRYPTO)//可选：是否加密，默认加密
            //.setProtoVersion(ProtoVersion.XQC_IDRAFT_VER_29)//根据服务端端支持协议切换，默认H3
            //.setAlpnType(AlpnType.ALPN_HQ)//支持协议切换，默认H3
            //.setAlpnName("hq-interop")//自定义协议名字，注意：针对ALPN_HQ生效，要跟服务端对应，避免链接不上的问题
            .reuse(SetCache.getReuse(applicationContext) == 1)//是否长链接复用，注意要看后端是否支持，能复用，强烈建议复用，在性能上会有非常大的提升，例如：阿里云这些是支持的，默认false
            //更多参数可以看详细的文档或者代码
            .build()

        textView = findViewById(R.id.tv_result)

        etContent = findViewById(R.id.et_content)

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {
            val testCount = SetCache.getTestCount(applicationContext)
            val methodGet = SetCache.getMethod(applicationContext) == "GET"
            val timeSpace = SetCache.getTestSpace(applicationContext)

            launch = CoroutineScope(Dispatchers.Default).launch {
                startTime = System.currentTimeMillis()
                successCount = 0
                failCount = 0
                atomicInteger.set(0)
                for (i in (1..testCount)) {
                    if (methodGet) {
                        get(i)
                    } else {
                        post(i)
                    }
                    XLogUtils.error("=====================index($i) ===============")

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

    }

    override fun onResume() {
        super.onResume()
        xquicClient.reuse = (SetCache.getReuse(applicationContext) == 1)
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
        val url = SetCache.getSelectUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }

        val content = etContent.text.toString()
        val xRequestBody =
            XRequestBody.create(XMediaType(XMediaType.MEDIA_TYPE_JSON), content)
        val xRequest = XRequest.Builder()
            .url("$url")
            .get(xRequestBody) //Default
            .addHeader("tenantId", "soacp")
            .addHeader("clientId", "portalApp")
            //.addHeader("testA", "testA")
            //.addHeader("Keep-Alive", "timeout=300, max=1000")
            .tag("index:$index")
            .build()
        request(index, xRequest)
    }

    private fun post(index: Int) {
        val url = SetCache.getSelectUrl(applicationContext)
        if (url.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "请先设置url", Toast.LENGTH_SHORT).show()
            return
        }

        val content = etContent.text.toString()
        val xRequestBody =
            XRequestBody.create(XMediaType(XMediaType.MEDIA_TYPE_JSON), content)
        val xRequest = XRequest.Builder()
            .url("$url")
            .post(xRequestBody) //Default
            .addHeader("tenantId", "soacp")
            .addHeader("clientId", "portalApp")
            .tag("index:$index")
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
            requestInfo.append("是否复用：" + xquicClient.reuse + "\n")
            appendText(requestInfo.toString())
        }

        xquicClient.newCall(xRequest).enqueue(object : XCallBack {
            override fun onFailure(call: XCall, exception: Exception) {
                exception.printStackTrace()
                XLogUtils.error(exception.message)

                synchronized(this@ShortConnActivity) {
                    failCount++
                }
                atomicInteger.incrementAndGet()
                if (atomicInteger.get() >= SetCache.getTestCount(applicationContext)) {
                    appendText(
                        "成功次数：${successCount},失败次数：${failCount},成功率:${
                            successCount / (SetCache.getTestCount(
                                applicationContext
                            ) * 1.0f)
                        } %,总耗时${System.currentTimeMillis() - startTime}"
                    )
                }
            }

            override fun onResponse(call: XCall, xResponse: XResponse, isFinish: Boolean) {
                parseResponse(startTime, index, xResponse, isFinish)
                synchronized(this@ShortConnActivity) {
                    successCount++
                }
                atomicInteger.incrementAndGet()
                if (atomicInteger.get() >= SetCache.getTestCount(applicationContext)) {
                    appendText(
                        "成功次数：${successCount},失败次数：${failCount},成功率:${
                            (successCount / (SetCache.getTestCount(
                                applicationContext
                            ) * 1.0f)) * 100
                        }%,总耗时${System.currentTimeMillis() - startTime} ms"
                    )
                }
            }
        })
    }

    private fun parseResponse(
        startTime: Long,
        index: Int,
        xResponse: XResponse,
        isFinish: Boolean
    ) {
        var content: String = xResponse.xResponseBody.body()
        if (content.length > 512 * 1024) {
            content = "数据太大，无法打印和显示，数据长度为:" + content.length
        }
        val now = System.currentTimeMillis()

        XLogUtils.error(
            "index=$index, java 总花费时长： ${(now - startTime)} ms,队列等待时长：${xResponse.delayTime} ms,isFinish=${isFinish},请求响应时长：${now - startTime - xResponse.delayTime} ms,size=${content.length},tag=${xResponse.xResponseBody.tag},content=${content}"
        )

        //appendText("$content ,index=$index, time=${now - startTime - xResponse.delayTime}ms ,status=" + xResponse.getStatus())
    }

    override fun onDestroy() {
        super.onDestroy()
        launch?.cancel()
        xquicClient.cancel("tag")
        val url = SetCache.getSelectUrl(applicationContext)
        url?.let {
            xquicClient.close(it)
        }
    }
}