package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.lizhi.component.net.xquic.native.XquicNative


class MainActivity : AppCompatActivity() {
    var clientCtx: Long = 0
    var isConnect: Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Thread {
            clientCtx = XquicNative.xquicInit()
            isConnect = XquicNative.xquicConnect(clientCtx, "192.168.23.10", 8443, "test", null)
            XquicNative.xquicStart(clientCtx)
        }.start()

        /*
        findViewById<Button>(R.id.btn_init).setOnClickListener {
            if (clientCtx == 0L) {
                clientCtx = XquicNative.xquicInit()
            }
        }

        findViewById<Button>(R.id.btn_connect).setOnClickListener {
            if (clientCtx == 0L) {
                Toast.makeText(applicationContext, "链接失败，请先Init", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            isConnect = XquicNative.xquicConnect(clientCtx, "192.168.23.10", 8443, "test", null)

            Thread {
                XquicNative.xquicStart(clientCtx)
            }.start()
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {

        }*/

        findViewById<Button>(R.id.btn_send_hq).setOnClickListener {
            if (isConnect < 0) {
                Toast.makeText(applicationContext, "请先connect", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            XquicNative.xquicH3Get(clientCtx, "Hello world hq")
        }

        findViewById<Button>(R.id.btn_send_h3).setOnClickListener {
            if (isConnect < 0) {
                Toast.makeText(applicationContext, "请先connect", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            XquicNative.xquicH3Post(clientCtx, "Hello world h3")
        }

        findViewById<Button>(R.id.btn_destroy).setOnClickListener {
            if (clientCtx == 0L) {
                Toast.makeText(applicationContext, "链接失败，请先Init", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            XquicNative.xquicDestroy(clientCtx)
        }
    }
}