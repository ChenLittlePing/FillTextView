package com.chenlittleping.filltextview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fillText.setText("大家好，我是<fill>请输入姓名</fill>，<fill></fill>, 这里可以点击<click>点我</click>，<underline>我有下划线</underline>")
        fillText.setEditTag("", "")
        fillText.setBlank("                  ")
        fillText.displayUnderline(true)
        fillText.setOnTextClickListener(View.OnClickListener {
            fillText.updateEditingText("我被点击了")
        })
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
