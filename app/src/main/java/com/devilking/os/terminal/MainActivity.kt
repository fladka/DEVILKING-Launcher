package com.devilking.os.terminal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "> DEVILKING OS: INITIALIZED\n> STATUS: SAFE_BOOT"
        tv.setBackgroundColor(0xFF000000.toInt())
        tv.setTextColor(0xFF00FF00.toInt())
        setContentView(tv)
    }
}
