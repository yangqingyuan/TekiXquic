package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lizhi.component.net.xquic.native.XquicNative


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val clientCtx: Long = XquicNative.xquicInit("127.0.0.1", 152, null, "session")
        XquicNative.xquicSend(clientCtx, XquicNative.SEND_TYPE_XQUIC, "test")
    }
}