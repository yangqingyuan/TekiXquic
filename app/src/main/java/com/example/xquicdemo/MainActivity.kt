package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.lizhi.component.net.xquic.native.XquicNative


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var clientCtx: Long  = XquicNative.xquicInit()
        XquicNative.xquicSend(clientCtx, "test")
        XquicNative.xquicConnect(clientCtx, "127.0.0.1", 122, null, null)
        XquicNative.xquicDestroy(clientCtx)

        findViewById<TextView>(R.id.sample_text).setOnClickListener {
            XquicNative.xquicDestroy(clientCtx)
        }
    }
}