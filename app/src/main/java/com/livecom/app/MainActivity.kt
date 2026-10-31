package com.livecom.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(12, 12, 14)
        val gold = Color.rgb(212, 175, 55)
        val muted = Color.rgb(190, 190, 190)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "LiveCom"
            textSize = 36f
            setTextColor(gold)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "مساحتك للبث المباشر والتواصل"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        val status = TextView(this).apply {
            text = "النسخة التأسيسية — جاري تجهيز خدمات الحسابات والغرف والبث"
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(8, 20, 8, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        setContentView(root)
    }
}
