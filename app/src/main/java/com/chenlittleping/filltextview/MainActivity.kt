package com.chenlittleping.filltextview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun clickBtn(view: View) {
        var t = ""
        for (text in fillText.getFillTexts()) {
            t += text
            t +=","
        }
        tv_fills.text = t.subSequence(0, t.length - 1)
    }
}
