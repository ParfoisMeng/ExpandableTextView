package com.parfoismeng.expandabletextviewlib.weiget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.annotation.Nullable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.TextView
import com.parfoismeng.expandabletextviewlib.R

/**
 * author : ParfoisMeng
 * time   : 2019/8/21
 * desc   : 展开/收起 TextView
 */
class ExpandableTextView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    /**
     * 内容文本 TextView
     */
    private val tvContent: TextView by lazy { TextView(context) }
    /**
     * 展开/收起 TextView
     */
    private val tvLabel: TextView by lazy { TextView(context) }

    /**
     * 内容文本
     */
    private var contentText = ""
    /**
     * 内容文本 字体颜色 默认黑色
     */
    private var contentTextColor = Color.BLACK
    /**
     * 内容文本 字体大小 默认14sp
     */
    private var contentTextSize = sp2px(14)
    /**
     * 内容文本 最多显示行数 默认4行
     */
    private var contentMaxLine = 4
    /**
     * 中间间距 默认2dp
     */
    private var middlePadding = dp2px(2)
    /**
     * 展开/收起 '展开'文本
     */
    private var labelExpandText: CharSequence = ""
    /**
     * 展开/收起 '收起'文本
     */
    private var labelCollapseText: CharSequence = ""
    /**
     * 展开/收起 字体颜色 默认蓝色
     */
    private var labelTextColor = Color.BLUE
    /**
     * 展开/收起 字体大小 默认12sp
     */
    private var labelTextSize = sp2px(12)
    /**
     * 展开/收起 '展开'图片 默认无
     */
    private var labelExpandDrawable: Drawable? = null
    /**
     * 展开/收起 '收起'图片 默认无
     */
    private var labelCollapseDrawable: Drawable? = null
    /**
     * 展开/收起 图片位置 默认POSITION_LEFT
     */
    private var labelDrawablePosition = POSITION_LEFT

    /**
     * 动画时长 默认300ms
     */
    private var animDuration = 300L

    /**
     * 是否处于收起状态
     */
    private var isCollapsed = true
    /**
     * 是否需要重新测量
     */
    private var isNeedReMeasure = true

    /**
     * 文本内容的真实高度
     */
    private var contentRealHeight = 0
    /**
     * 收起时候的整体高度
     */
    private var collapsedHeight = 0
    /**
     * 除去文本内容的剩余高度
     */
    private var otherHeight = 0
    /**
     * 是否正在执行动画
     */
    private var isAnimating = false

    var onExpandStateChangeListener: OnExpandStateChangeListener? = null

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextView)
        contentText = array.getString(R.styleable.ExpandableTextView_contentText) ?: ""
        contentTextColor = array.getColor(R.styleable.ExpandableTextView_contentTextColor, Color.BLACK)
        contentTextSize = array.getDimensionPixelOffset(R.styleable.ExpandableTextView_contentTextSize, sp2px(14))
        contentMaxLine = array.getInteger(R.styleable.ExpandableTextView_contentMaxLine, 4)
        middlePadding = array.getDimensionPixelOffset(R.styleable.ExpandableTextView_middlePadding, dp2px(2))
        labelExpandText = array.getString(R.styleable.ExpandableTextView_labelExpandText) ?: ""
        labelCollapseText = array.getString(R.styleable.ExpandableTextView_labelCollapseText) ?: ""
        labelTextColor = array.getColor(R.styleable.ExpandableTextView_labelTextColor, Color.BLUE)
        labelTextSize = array.getDimensionPixelOffset(R.styleable.ExpandableTextView_labelTextSize, sp2px(12))
        labelExpandDrawable = array.getDrawable(R.styleable.ExpandableTextView_labelExpandDrawable)
        labelCollapseDrawable = array.getDrawable(R.styleable.ExpandableTextView_labelCollapseDrawable)
        labelDrawablePosition = array.getInteger(R.styleable.ExpandableTextView_labelDrawablePosition, POSITION_LEFT)
        animDuration = array.getInteger(R.styleable.ExpandableTextView_animDuration, 300).toLong()
        array.recycle()

        orientation = VERTICAL
        addView(tvContent.apply {
            text = contentText
            setTextColor(contentTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, contentTextSize.toFloat())
            ellipsize = TextUtils.TruncateAt.END
        })
        addView(tvLabel.apply {
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(labelTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize.toFloat())
            setPadding(0, middlePadding, 0, 0)

            text = labelExpandText
            labelExpandDrawable?.let {
                when (labelDrawablePosition) {
                    POSITION_LEFT -> setCompoundDrawableLeft(it)
                    POSITION_RIGHT -> setCompoundDrawableRight(it)
                }
            }
        })
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        tvLabel.setOnClickListener(onExpandClickListener)
    }

    private val onExpandClickListener = object : OnClickListener {
        private var animation: ExpandCollapseAnimation? = null
        override fun onClick(v: View?) {
            isCollapsed = !isCollapsed

            if (isCollapsed) {
                tvLabel.text = labelExpandText

                labelExpandDrawable?.let {
                    when (labelDrawablePosition) {
                        POSITION_LEFT -> tvLabel.setCompoundDrawableLeft(it)
                        POSITION_RIGHT -> tvLabel.setCompoundDrawableRight(it)
                    }
                }
                animation = ExpandCollapseAnimation(this@ExpandableTextView.height, collapsedHeight)
            } else {
                tvLabel.text = labelCollapseText
                labelCollapseDrawable?.let {
                    when (labelDrawablePosition) {
                        POSITION_LEFT -> tvLabel.setCompoundDrawableLeft(it)
                        POSITION_RIGHT -> tvLabel.setCompoundDrawableRight(it)
                    }
                }
                animation = ExpandCollapseAnimation(this@ExpandableTextView.height, contentRealHeight + otherHeight)
            }

            clearAnimation()
            animation?.apply {
                fillAfter = true
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        isAnimating = true
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        clearAnimation()
                        isAnimating = false

                        tvContent.maxLines = if (isCollapsed) {
                            contentMaxLine
                        } else {
                            Int.MAX_VALUE
                        }

                        onExpandStateChangeListener?.onExpandStateChanged(isCollapsed)
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                    }
                })
                startAnimation(animation)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 如果隐藏控件或者TextView的值没有发生改变，那么不进行测量
        if (visibility == View.GONE || !isNeedReMeasure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        isNeedReMeasure = false

        // 初始化默认状态，即正常显示文本
        tvLabel.visibility = View.GONE
        tvContent.maxLines = Integer.MAX_VALUE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // 如果本身没有达到收起展开的限定要求，则不进行处理
        if (tvContent.lineCount <= contentMaxLine) {
            return
        }

        // 初始化高度赋值，为后续动画事件准备数据
        contentRealHeight = tvContent.measuredHeight

        // 如果处于收缩状态，则设置最多显示行数
        if (isCollapsed) {
            tvContent.maxLines = contentMaxLine
        }
        tvLabel.visibility = View.VISIBLE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (isCollapsed) {
            post {
                collapsedHeight = measuredHeight
                otherHeight = tvLabel.measuredHeight + paddingBottom + paddingTop
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 执行动画的过程中屏蔽事件
        return isAnimating
    }

    /**
     * 设置内容文本最多显示行数
     */
    fun setContentMaxLine(maxLine: Int) {
        contentMaxLine = maxLine

        reMeasure()
    }

    /**
     * 设置内容文本
     */
    fun setContentText(text: CharSequence) {
        tvContent.text = text

        reMeasure()
    }

    /**
     * 设置是否收起
     */
    fun setCollapsed(isCollapsed: Boolean) {
        this.isCollapsed = isCollapsed

        reMeasure()
    }

    /**
     * 设置按钮文字
     * @param expandText '展开'文本
     * @param collapseText '收起'文本
     */
    fun setLabelText(expandText: CharSequence, collapseText: CharSequence) {
        labelExpandText = expandText
        labelCollapseText = collapseText

        tvLabel.text = if (isCollapsed) {
            labelExpandText
        } else {
            labelCollapseText
        }
    }

    /**
     * 设置按钮图标
     * @param expandDrawable '展开'图标
     * @param collapseDrawable '收起'图标
     */
    fun setLabelDrawable(expandDrawable: Drawable?, collapseDrawable: Drawable?) {
        labelExpandDrawable = expandDrawable
        labelCollapseDrawable = collapseDrawable

        if (isCollapsed) {
            labelExpandDrawable?.let {
                when (labelDrawablePosition) {
                    POSITION_LEFT -> tvLabel.setCompoundDrawableLeft(it)
                    POSITION_RIGHT -> tvLabel.setCompoundDrawableRight(it)
                }
            }
        } else {
            labelCollapseDrawable?.let {
                when (labelDrawablePosition) {
                    POSITION_LEFT -> tvLabel.setCompoundDrawableLeft(it)
                    POSITION_RIGHT -> tvLabel.setCompoundDrawableRight(it)
                }
            }
        }
    }

    /**
     * 设置内容文本字体颜色
     */
    fun setContentTextColor(color: Int) {
        tvContent.setTextColor(color)
    }

    /**
     * 设置内容文本字体大小
     */
    fun setContentTextSize(px: Int) {
        tvContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, px.toFloat())
    }

    /**
     * 设置展开/收起字体颜色
     */
    fun setLabelTextColor(color: Int) {
        tvLabel.setTextColor(color)
    }

    /**
     * 设置展开/收起字体大小
     */
    fun setLabelTextSize(px: Int) {
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, px.toFloat())
    }

    private fun reMeasure() {
        isNeedReMeasure = true

        clearAnimation()
        requestLayout()
    }

    private inner class ExpandCollapseAnimation(val startValue: Int, val endValue: Int) : Animation() {
        init {
            duration = animDuration
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            val height = ((endValue - startValue) * interpolatedTime + startValue).toInt()
            tvContent.maxHeight = height - otherHeight
//            this@ExpandableTextView.layoutParams.height = height
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    interface OnExpandStateChangeListener {
        fun onExpandStateChanged(isExpanded: Boolean)
    }

    private fun dp2px(dpValue: Int): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5F).toInt()
    }

    private fun sp2px(spValue: Int): Int {
        val scale = resources.displayMetrics.scaledDensity
        return (spValue * scale + 0.5F).toInt()
    }

    private fun TextView.setCompoundDrawableLeft(left: Drawable?) {
        val drawableArr = compoundDrawables
        left?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            setCompoundDrawables(this, drawableArr[1], drawableArr[2], drawableArr[3])
        }
    }

    private fun TextView.setCompoundDrawableRight(right: Drawable?) {
        val drawableArr = compoundDrawables
        right?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            setCompoundDrawables(drawableArr[0], drawableArr[1], right, drawableArr[3])
        }
    }

    companion object {
        private const val POSITION_LEFT = 0
        private const val POSITION_RIGHT = 1
    }
}