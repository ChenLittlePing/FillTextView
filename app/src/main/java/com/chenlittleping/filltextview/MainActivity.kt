package com.chenlittleping.filltextview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        fillText.setText("你好我是，我来自&#060;fill&#062;。")
    }

    fun clickBtn(view: View) {
        var t = ""
        for (text in fillText.getFillTexts()) {
            t += text
            t +=","
        }
        Toast.makeText(this, t.subSequence(0, t.length - 1), Toast.LENGTH_LONG).show()
    }
}
