package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.lizhi.component.net.xquic.listener.XquicCallback
import com.lizhi.component.net.xquic.native.XquicShortNative


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.btn_init).setOnClickListener {

        }

        findViewById<Button>(R.id.btn_connect).setOnClickListener {

        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {

        }

        findViewById<Button>(R.id.btn_send_hq).setOnClickListener {

            Thread {
                val startTime = System.currentTimeMillis()
                XquicShortNative().send(
                    "https://192.168.10.245:8443",
                    null,
                    null,
                    "我是测试",
                    object : XquicCallback {
                        override fun callBack(ret: Int, data: ByteArray) {
                            Log.e(
                                "LzXquic->jni",
                                "花费时间 ${(System.currentTimeMillis() - startTime)} ms ,ret=$ret , data:${
                                    String(data)
                                }"
                            )
                        }
                    },
                )
                Log.e("LzXquic->jni", "整个过程花费时间：" + (System.currentTimeMillis() - startTime)+" ms")
            }.start()
        }

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {

        }

        findViewById<Button>(R.id.btn_destroy).setOnClickListener {

        }
    }
}