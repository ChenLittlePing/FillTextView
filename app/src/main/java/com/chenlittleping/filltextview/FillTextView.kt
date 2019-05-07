package com.chenlittleping.filltextview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_DEL
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager


/**
 * 文本填空控件
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @Datetime 2019-04-28 15:02
 *
 */
class FillTextView: View, MyInputConnection.InputListener, View.OnKeyListener {

    //编辑字段标记
    private var EDIT_TAG = "<fill>"

    //编辑字段替换
    private var EDIT_REPLACEMENT = "【        】"

    //可编辑空白
    private val BLANKS = "        "

    //可编辑开始符
    private var mEditStartTag = "【"

    //可编辑结束符
    private var mEditEndTag = "】"

    //文本
    private var mText = StringBuffer()

    //存放文字段的列表，根据<fill>分割为多个字段
    private var mTextList = arrayListOf<AText>()

    //正在输入的字段
    private var mEditingText: AText? = null

    //当前正在编辑的文本行数
    private var mEditTextRow = 1

    //光标[0]：x坐标,[1]：文字的基准线
    private var mCursor = arrayOf(0f, 0f)

    //光标所在文字索引
    private var mCursorIndex = 0

    //光标闪烁标志
    private var mHideCursor = true

    //控件宽度
    private var mWidth = 0

    //文字画笔
    private val mNormalPaint = Paint()

    //普通文字颜色
    private var mNormalColor = Color.BLACK

    //文字画笔
    private val mFillPaint = Paint()

    //填写文字颜色
    private var mFillColor = Color.BLACK

    //光标画笔
    private val mCursorPain = Paint()

    //一个汉字的宽度
    private var mOneWordWidth = 0f

    //一行最大的文字数
    private var mMaxSizeOneLine = 0

    //字体大小
    private var mTextSize = 16f

    //当前绘制到第几行
    private var mCurDrawRow = 1

    //获取文字的起始位置
    private var mStartIndex = 0

    //获取文字的结束位置
    private var mEndIndex = 0

    //存放每行的文字，用于计算文字长度
    private var mOneRowText = StringBuffer()

    //一行字包含的字段：普通字段，可编辑字段
    private var mOneRowTexts = arrayListOf<AText>()

    //默认行距2dp
    private var mRowSpace = 2f

    constructor(context: Context): super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        getAttrs(attrs)
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        getAttrs(attrs)
        init()
    }

    private fun getAttrs(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.filled_text)
        mTextSize = ta.getFloat(R.styleable.filled_text_textSize, mTextSize)
        mText = mText.append(ta.getText(R.styleable.filled_text_filledText))
        mNormalColor = ta.getColor(R.styleable.filled_text_normalColor, Color.BLACK)
        mFillColor = ta.getColor(R.styleable.filled_text_fillColor, Color.BLACK)
        mRowSpace += ta.getFloat(R.styleable.filled_text_rowSpace, 0f)
        ta.recycle()
    }

    private fun init() {
        initCursorPaint()
        initTextPaint()
        initFillPaint()
        splitTexts()
        initHandler()
        setOnKeyListener(this)
    }

    /**
     * 初始化光标画笔
     */
    private fun initCursorPaint() {
        mCursorPain.strokeWidth = 3f
        mCursorPain.color = mFillColor
        mCursorPain.isAntiAlias = true
    }

    /**
     * 初始化文字画笔
     */
    private fun initTextPaint() {
        mTextSize = sp2px(mTextSize).toFloat()
        mRowSpace = dp2px(mRowSpace).toFloat()

        mNormalPaint.color = mNormalColor
        mNormalPaint.textSize = mTextSize
        mNormalPaint.isAntiAlias = true

        mOneWordWidth = measureTextLength("测")
    }

    private fun initFillPaint() {
        mFillPaint.color = mFillColor
        mFillPaint.textSize = mTextSize
        mFillPaint.isAntiAlias = true
    }

    private fun dp2px(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5).toInt()
    }

    private fun sp2px(sp: Float): Int {
        val density = resources.displayMetrics.scaledDensity
        return (sp * density + 0.5).toInt()
    }

    /**
     * 拆分文字，普通文字和可编辑文字
     */
    private fun splitTexts() {
        mTextList.clear()
        val texts = mText.split(EDIT_TAG)
        for (i in 0 until texts.size - 1) {
            val aText = AText(texts[i])
            mTextList.add(aText)
            val fillText = AText(EDIT_REPLACEMENT, true)
            mTextList.add(fillText)
        }
        mTextList.add(AText(texts[texts.size - 1]))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mMaxSizeOneLine = (w / mOneWordWidth).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var width = widthSize
        var height = heightSize

        when(widthMode) {
            MeasureSpec.EXACTLY -> width = widthSize
            MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST ->
                width = Math.min(widthSize, measureTextLength(mText.toString()).toInt())
        }

        when(heightMode) {
            MeasureSpec.EXACTLY -> height = heightSize
            MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST ->
                height = Math.min(heightSize, //其中mRowSpace + mNormalPaint.fontMetrics.descent最后一行距离底部的间距
                        (getRowHeight() * (mCurDrawRow - 1) + mRowSpace + mNormalPaint.fontMetrics.descent).toInt())
        }
        setMeasuredDimension(width, height)
    }

    override fun draw(canvas: Canvas) {
        clear()
        canvas.save()
        mStartIndex = 0
        mEndIndex = mMaxSizeOneLine
        for (i in 0 until mTextList.size) {
            val aText = mTextList[i]
            val text = aText.text
            while (true) {
                if (mEndIndex > text.length) {
                    mEndIndex = text.length
                }
                addEditStartPos(aText) //记录编辑初始位置

                val cs = text.subSequence(mStartIndex, mEndIndex)
                mOneRowTexts.add(AText(cs.toString(), aText.isFill))
                mOneRowText.append(cs)

                val textWidth = measureTextLength(mOneRowText.toString())
                if (textWidth <= mWidth) {
                    val left = mWidth - textWidth
                    val textCount = left / mOneWordWidth
                    if (mEndIndex < text.length) {
                        mStartIndex = mEndIndex
                        mEndIndex += textCount.toInt()
                        if (mStartIndex == mEndIndex) {
                            val one = measureTextLength(text.substring(mEndIndex, mEndIndex + 1))
                            if (one + textWidth < mWidth) { //可以放多一个字
                                mEndIndex++
                            } else {
                                //绘制文字
                                addEditEndPos(aText)
                                drawOneRow(canvas)
                                addEditStartPosFromZero(aText, mStartIndex) //编辑的段落可能进入下一行
                            }
                        }
                    } else { //进入下一段文字
                        addEditEndPos(aText) //记录编辑结束位置
                        if (i < mTextList.size - 1) {
                            mStartIndex = 0
                            mEndIndex = textCount.toInt()
                            if (mStartIndex == mEndIndex) {
                                val one = measureTextLength(mTextList[i + 1].text.substring(0, 1))
                                if (one + textWidth < mWidth) { //可以放多一个字
                                    mEndIndex = 1 //只读下一段文字第一个字
                                } else {
                                    //绘制文字
                                    drawOneRow(canvas)
                                }
                            }
                        } else {
                            //绘制文字
                            drawOneRow(canvas)
                        }
                        break
                    }
                } else {
                    //绘制文字
                    drawOneRow(canvas)
                }
            }
        }
        drawCursor(canvas)
        super.draw(canvas)
        canvas.restore()
    }

    private var mHandler: Handler? = null

    /**
     * 光标闪烁定时
     */
    private fun initHandler() {
        mHandler = Handler(Handler.Callback {
            mHideCursor = !mHideCursor
            mHandler!!.sendEmptyMessageDelayed(1, 500)
            invalidate()
            true
        })
        mHandler!!.sendEmptyMessageDelayed(1, 500)
    }

    /**
     * 清除过期状态
     */
    private fun clear() {
        mCurDrawRow = 1
        mStartIndex = 0
        mEndIndex = 0
        mOneRowText.delete(0, mOneRowText.length)
        mOneRowTexts.clear()
        mEditingText?.posInfo?.clear()
    }

    /**
     * 绘制一行文字
     */
    private fun drawOneRow(canvas: Canvas) {
        //drawText中的y坐标为文字基线
//        canvas.drawText(mOneRowText.toString(), 0f, getRowHeight()*mCurDrawRow, mNormalPaint)

        var x = 0f
        for (aText in mOneRowTexts) {
            canvas.drawText(aText.text, x, getRowHeight()*mCurDrawRow,
                            if (aText.isFill) mFillPaint else mNormalPaint)
            x += measureTextLength(aText.text)
        }

        mCurDrawRow++
        mEndIndex += mMaxSizeOneLine
        mOneRowText.delete(0, mOneRowText.length)
        mOneRowTexts.clear()
        requestLayout()
    }

    /**
     * 绘制光标
     */
    private fun drawCursor(canvas: Canvas) {
        if (mHideCursor) {
            mCursorPain.alpha = 0
        } else {
            mCursorPain.alpha = 255
        }

        if (mCursor[0] > 0 && mCursor[1] >= 0) {
            val fm = mNormalPaint.fontMetrics //文字基准线问题
            canvas.drawLine(mCursor[0], mCursor[1] + fm.ascent,
                    mCursor[0], (mCursor[1] + fm.descent), mCursorPain)
        }
    }

    /**
     * 添加编辑字段起始位置
     */
    private fun addEditStartPos(aText: AText) {
        if (aText.isFill && mStartIndex == 0) {
            val width = measureTextLength(mOneRowText.toString()).toInt()
            val rect = Rect(width, (getRowHeight()*(mCurDrawRow - 1) + mRowSpace/*加上行距*/).toInt(), 0, 0)
            val info = EditPosInfo(mStartIndex, rect)
            aText.posInfo[mCurDrawRow] = info
        }
    }

    /**
     * 添加编辑字段起始位置（换行的情况）
     */
    private fun addEditStartPosFromZero(aText: AText, index: Int) {
        if (aText.isFill) {
            val rect = Rect(0, (getRowHeight()*(mCurDrawRow - 1) + mRowSpace/*加上行距*/).toInt(), 0, 0)
            val info = EditPosInfo(index, rect)
            aText.posInfo[mCurDrawRow] = info
        }
    }

    /**
     * 添加编辑字段结束位置
     */
    private fun addEditEndPos(aText: AText) {
        if (aText.isFill) {
            val width = measureTextLength(mOneRowText.toString())
            aText.posInfo[mCurDrawRow]?.rect?.right = width.toInt()
            aText.posInfo[mCurDrawRow]?.rect?.bottom = (getRowHeight()*mCurDrawRow).toInt()
        }
    }

    /**
     * 计算文字长度：px
     */
    private fun measureTextLength(text: String): Float {
        return mNormalPaint.measureText(text)
    }

    /**
     * 获取一行高度
     */
    private fun getRowHeight(): Float {
        return mTextSize + mRowSpace
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            hideInput()
        }
    }

    /**
     * 隐藏输入法
     */
    private fun hideInput() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (touchCollision(event)) {
                    isFocusableInTouchMode = true //important
                    isFocusable = true
                    requestFocus()
                    try {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(this, InputMethodManager.RESULT_SHOWN)
                        imm.restartInput(this)
                    } catch (ignore: Exception) {
                    }
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 检测点击碰撞
     */
    private fun touchCollision(event: MotionEvent): Boolean {
        for (aText in mTextList) {
            if (aText.isFill) {
                for ((row, posInfo) in aText.posInfo) {
                    if (event.x > posInfo.rect.left && event.x < posInfo.rect.right &&
                        event.y > posInfo.rect.top && event.y < posInfo.rect.bottom) {
                        mEditTextRow = row
                        if (aText.text == EDIT_REPLACEMENT) {
                            val firstRow = aText.getStartPos()
                            if (firstRow >= 0) { //可能存在换行
                                mEditTextRow = firstRow
                            }
                        }
                        mEditingText = aText
                        calculateCursorPos(event, aText.posInfo[mEditTextRow]!!, aText.text)
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 计算光标位置
     */
    private fun calculateCursorPos(event: MotionEvent, posInfo: EditPosInfo, text: String) {
        val eX = event.x
        var innerWidth = eX - posInfo.rect.left
        var nWord = (innerWidth / mOneWordWidth).toInt()
        var wordsWidth = 0
        if (nWord <= 0) nWord = 1
        if (text == EDIT_REPLACEMENT) {
            mCursor[0] = (posInfo.rect.left + mTextSize)
            mCursor[1] = posInfo.rect.bottom.toFloat()
        } else {
            //循环计算，直到最后一个真正超过显示范围的文字（因为汉字和英文数字占位不一样，这里以汉字作为初始占位）
            do {
                wordsWidth = measureTextLength(text.substring(posInfo.index, posInfo.index + nWord)).toInt()
                nWord++
            } while (wordsWidth < innerWidth && posInfo.index + nWord < text.length)
            mCursorIndex = posInfo.index + nWord - 1
            mCursor[0] = posInfo.rect.left + measureTextLength(text.substring(posInfo.index, mCursorIndex))
            mCursor[1] = posInfo.rect.bottom.toFloat()
        }
        invalidate()
    }

    /**
     * 键盘输入
     */
    override fun onTextInput(text: CharSequence) {
        if (mEditingText != null) {
            val filledText = StringBuffer(mEditingText!!.text.replace(EDIT_REPLACEMENT, ""))
            if (filledText.isEmpty()) {
                filledText.append(text)
                mCursorIndex = text.length + 1
            } else {
                filledText.insert(mCursorIndex, text)
                        .replace(0,1,"")
                        .replace(filledText.length - 1, filledText.length, "")
                mCursorIndex += text.length
            }
            mEditingText!!.text = "【$filledText】"
            if (mCursor[0] + measureTextLength(text.toString()) > mWidth) {//计算实际可以放多少字
                var restCount = ((mWidth - mCursor[0])/mOneWordWidth).toInt()
                var realWidth = mCursor[0] + measureTextLength(text.substring(0, restCount))

                //循环计算，直到最后一个真正超过显示范围的文字（因为汉字和英文数字占位不一样，这里以汉字作为初始占位）
                while (realWidth <= mWidth && restCount < text.length) {
                    restCount++
                    realWidth = mCursor[0] + measureTextLength(text.substring(0, restCount))
                }
                mEditTextRow += ((mCursor[0] + measureTextLength(text.toString()))/mWidth).toInt()
                if (mEditTextRow < 1) mEditTextRow = 1
                val realCount = if (restCount - 1 > 0) restCount -1 else 0
                mCursor[0] = measureTextLength(text.substring(realCount, text.length))
                mCursor[1] = getRowHeight() * (mEditTextRow)
            } else {
                mCursor[0] += measureTextLength(text.toString())
            }
            invalidate()
        }
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE
        return MyInputConnection(this, false, this)
    }

    override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL &&
            keyEvent.action == KeyEvent.ACTION_DOWN) {
            onDeleteWord()
            return true
        }
        return false
    }

    override fun onDeleteWord() {
        if (mEditingText != null) {
            val text = StringBuffer(mEditingText?.text)
            if (!text.isNullOrEmpty() && text.length > 1) {
                if (mCursorIndex > 1) {
                    var cursorPos = (mCursor[0] - measureTextLength(text.substring(mCursorIndex - 1, mCursorIndex))).toInt()
                    if (cursorPos >= 0) {//光标仍然在同一行
                        mCursor[0] = cursorPos.toFloat()
                    } else { //光标回到上一行
                        mEditTextRow--
                        val posInfo = mEditingText!!.posInfo[mEditTextRow]!!
                        mCursor[0] = posInfo.rect.left + measureTextLength(text.substring(posInfo.index, mCursorIndex - 1))
                        mCursor[1] = getRowHeight() * (mEditTextRow)
                    }
                    mEditingText?.text = text.replace(mCursorIndex - 1, mCursorIndex, "").toString()
                    mCursorIndex--

                    if (mEditingText?.text?.length?:0 < 3) {
                        if (text.toString() != EDIT_REPLACEMENT) {
                            mEditingText?.text = EDIT_REPLACEMENT
                            mCursorIndex = 1
                            val firstRow = mEditingText!!.getStartPos()
                            if (firstRow > 0) {//可能存在换行
                                mEditTextRow = firstRow
                            }
                            mCursor[0] = mEditingText!!.posInfo[mEditTextRow]!!.rect.left + mTextSize
                            mCursor[1] = getRowHeight() * (mEditTextRow)
                        }
                    }

                    invalidate()
                }
            }
        }
    }

    /**
     * 设置文本
     */
    fun setText(text: String) {
        mText = StringBuffer(text)
        splitTexts()
        invalidate()
    }

    /**
     * 设置字体大小，单位sp
     */
    fun setTextSize(sp: Float) {
        mTextSize = sp
        initTextPaint()
        initFillPaint()
        invalidate()
    }

    /**
     * 设置行距，单位dp
     */
    fun setRowSpace(dp: Float) {
        mRowSpace = dp2px(dp).toFloat()
        invalidate()
    }

    /**
     * 获取填写的文本内容
     */
    fun getFillTexts(): List<String> {
        val list = arrayListOf<String>()
        for (value in mTextList) {
            if (value.isFill) {
                list.add(value.text.replace("【", "").replace("】", ""))
            }
        }
        return list
    }
}

internal class MyInputConnection(targetView: View, fullEditor: Boolean, private val mListener: InputListener) : BaseInputConnection(targetView, fullEditor) {
    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        mListener.onTextInput(text)
        return super.commitText(text, newCursorPosition)
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        //软键盘的删除键 DEL 无法直接监听，自己发送del事件
        return if (beforeLength == 1 && afterLength == 0) {
            super.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_DEL)) && super.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        } else super.deleteSurroundingText(beforeLength, afterLength)
    }

    interface InputListener {
        fun onTextInput(text: CharSequence)
        fun onDeleteWord()
    }
}

/**
 * 文字段落
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @Datetime 2019-04-29 09:27
 *
 */
internal class AText(text: String, isFill: Boolean = false) {
    //段落文字
    var text: String = text

    //是否为填写字段
    var isFill = isFill

    //文本位置信息<行，文本框>
    var posInfo: MutableMap<Int, EditPosInfo> = mutableMapOf()

    fun getStartPos(): Int {
        if (posInfo.isEmpty()) return -1
        var firstRow = Int.MAX_VALUE
        for ((row, _) in posInfo) {
            if (firstRow > row) {
                firstRow = row
            }
        }
        return firstRow
    }
}

data class EditPosInfo(var index: Int,
                       var rect: Rect)