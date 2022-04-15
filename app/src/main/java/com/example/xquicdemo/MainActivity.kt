package com.example.xquicdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.btn_short).setOnClickListener {
            val intent = Intent(this, ShortConnActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_long).setOnClickListener {
            val intent = Intent(this, LongConnActivity::class.java)
            startActivity(intent)
        }
    }

}