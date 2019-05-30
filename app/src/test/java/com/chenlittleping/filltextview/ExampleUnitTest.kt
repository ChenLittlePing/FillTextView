package com.chenlittleping.filltextview

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

        val s = "大家好，我是<fill>请输入姓名<fill/>，<fill><fill/>, 这里可以点击<click>点我<click/>，<underline>我加了下划线<underline/>"

        val a = s.split("<fill/>", "<click/>", "<underline/>")
        for (b in a) {
            print("b:$b\n")
        }
    }
}
