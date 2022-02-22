package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.lizhi.component.net.xquic.native.XquicNative


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var clientCtx: Long = XquicNative.xquicInit()
        Thread {
            XquicNative.xquicConnect(clientCtx, "192.168.23.10", 8443, "test", null)
        }.start()

        findViewById<TextView>(R.id.sample_text).setOnClickListener {
            XquicNative.xquicDestroy(clientCtx)
        }
    }
}