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
    private lateinit var etUrl1: EditText
    private lateinit var etUrl2: EditText
    private lateinit var etTestCount: EditText
    private lateinit var etTestSpace: EditText

    private lateinit var checkBox: CheckBox
    private lateinit var checkBox1: CheckBox
    private lateinit var checkBox2: CheckBox
    private lateinit var cb_reuse: CheckBox

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

        etUrl1 = findViewById(R.id.etUrl1)
        etUrl1.text.append(SetCache.getUrl1(applicationContext) ?: "")

        etUrl2 = findViewById(R.id.etUrl2)
        etUrl2.text.append(SetCache.getUrl2(applicationContext) ?: "")


        etTestCount = findViewById(R.id.etTestCount)
        etTestCount.text.append("" + SetCache.getTestCount(applicationContext))


        etTestSpace = findViewById(R.id.etTestSpace)
        etTestSpace.text.append("" + SetCache.getTestSpace(applicationContext))


        findViewById<Button>(R.id.btn_ok).setOnClickListener {

            if (etTimeout.text.isNotEmpty()) {
                SetCache.setConnTimeOut(application, etTimeout.text.toString().toInt())
            }

            if (etUrl.text.isNotEmpty()) {
                SetCache.setUrl(applicationContext, etUrl.text.toString())
            }


            if (etUrl1.text.isNotEmpty()) {
                SetCache.setUrl1(applicationContext, etUrl1.text.toString())
            }

            if (etUrl2.text.isNotEmpty()) {
                SetCache.setUrl2(applicationContext, etUrl2.text.toString())
            }


            if (etTestCount.text.isNotEmpty()) {
                val count = etTestCount.text.toString().toInt()
                if (count in 1..Int.MAX_VALUE) {
                    SetCache.setTestCount(applicationContext, count)
                }
            }


            if (etTestSpace.text.isNotEmpty()) {
                val count = etTestSpace.text.toString().toInt()
                if (count in 0..100) {
                    SetCache.setTestSpace(applicationContext, count)
                }
            }

            Toast.makeText(applicationContext, "设置成功", Toast.LENGTH_SHORT).show()
            finish()
        }

        checkBox = findViewById(R.id.checkbox)
        checkBox.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                checkBox1.isChecked = false
                checkBox2.isChecked = false
                SetCache.setSelect(applicationContext, 0)
            }

        }
        checkBox1 = findViewById(R.id.checkbox1)
        checkBox1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBox.isChecked = false
                checkBox2.isChecked = false
                SetCache.setSelect(applicationContext, 1)
            }
        }
        checkBox2 = findViewById(R.id.checkbox2)
        checkBox2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBox1.isChecked = false
                checkBox.isChecked = false
                SetCache.setSelect(applicationContext, 2)
            }
        }

        when (SetCache.getSelect(applicationContext)) {
            0 -> {
                checkBox.isChecked = true
            }
            1 -> {
                checkBox1.isChecked = true
            }
            2 -> {
                checkBox2.isChecked = true
            }
        }

        cb_reuse = findViewById(R.id.cb_reuse)
        if (SetCache.getReuse(applicationContext) == 1) {
            cb_reuse.isChecked = true
        }
        cb_reuse.setOnCheckedChangeListener { buttonView, isChecked ->
            SetCache.setReuse(applicationContext, 1)
        }

    }

}