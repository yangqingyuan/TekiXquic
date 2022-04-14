package com.example.xquicdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.lizhi.component.net.xquic.native.CCType
import com.lizhi.component.net.xquic.utils.XLogUtils

class SetActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var rgMethod: RadioGroup
    private lateinit var etTimeout: EditText
    private lateinit var etUrl: EditText
    private lateinit var etTestCount: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set)

        radioGroup = findViewById(R.id.rg_cc_type)
        radioGroup.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.rb_bbr -> {
                    SetCache.setCCType(application, CCType.BBR)
                    XLogUtils.info("BBR")
                }
                R.id.rb_cubic -> {
                    SetCache.setCCType(application, CCType.CUBIC)
                    XLogUtils.info("CUBIC")
                }
                else -> {
                    SetCache.setCCType(application, CCType.RENO)
                    XLogUtils.info("ccType")
                }
            }
        }
        when (SetCache.getCCType(applicationContext)) {
            CCType.BBR -> {
                (findViewById<RadioButton>(R.id.rb_bbr)).isChecked = true
            }
            CCType.CUBIC -> {
                (findViewById<RadioButton>(R.id.rb_cubic)).isChecked = true
            }
            else -> {
                (findViewById<RadioButton>(R.id.rb_beon)).isChecked = true
            }
        }


        rgMethod = findViewById(R.id.rg_method)
        rgMethod.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.rb_get -> {
                    SetCache.setMethod(application, "GET")
                    XLogUtils.info("GET")
                }
                else -> {
                    SetCache.setMethod(application, "POST")
                    XLogUtils.info("ccType")
                }
            }
        }
        when (SetCache.getMethod(applicationContext)) {
            "GET" -> {
                (findViewById<RadioButton>(R.id.rb_get)).isChecked = true
            }
            else -> {
                (findViewById<RadioButton>(R.id.rb_post)).isChecked = true
            }
        }

        etTimeout = findViewById(R.id.etTimeout)
        etTimeout.text.append("" + SetCache.getConnTimeout(applicationContext))

        etUrl = findViewById(R.id.etUrl)
        etUrl.text.append(SetCache.getUrl(applicationContext) ?: "")

        etTestCount = findViewById(R.id.etTestCount)
        etTestCount.text.append("" + SetCache.getTestCount(applicationContext))


        findViewById<Button>(R.id.btn_ok).setOnClickListener {

            if (etTimeout.text.isNotEmpty()) {
                SetCache.setConnTimeOut(application, etTimeout.text.toString().toInt())
            }

            if (etUrl.text.isNotEmpty()) {
                SetCache.setUrl(applicationContext, etUrl.text.toString())
            }

            if (etTestCount.text.isNotEmpty()) {
                val count = etTestCount.text.toString().toInt()
                if (count in 1..1000) {
                    SetCache.setTestCount(applicationContext, count)
                }
            }

            Toast.makeText(applicationContext, "设置成功", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

}