package com.tz.drawpicture

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class MyImageView : View {

    companion object {
        private val TAG = "MyImageView"
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context?, attributeSet: AttributeSet, defStyle: Int) : super(
        context,
        attributeSet,
        -1
    )

    private var mBitmap: Bitmap
    private var mPaint: Paint? = null
    private var realWidth = 0
    private var realHeight = 0


    init {

        mPaint = Paint()
        mPaint?.color = Color.GREEN
        mPaint?.flags = Paint.ANTI_ALIAS_FLAG
        mPaint?.style = Paint.Style.STROKE

        val op = BitmapFactory.Options()
        op.inJustDecodeBounds = true
        BitmapFactory.decodeResource(context.resources, R.drawable.live_ad_5, op)
        realWidth = op.outWidth
        realHeight = op.outHeight
        mBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.live_ad_5)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val ww = MeasureSpec.getSize(widthMeasureSpec)
        val pH = ww * realHeight / realWidth
        setMeasuredDimension(ww, pH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i(TAG, "onSizeChanged: $w  $h  $oldw  $oldh")

        val pH = w * realHeight / realWidth
        mBitmap = Bitmap.createScaledBitmap(mBitmap, w, pH, true)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawBitmap(mBitmap, 0f, 0f, mPaint)
    }
}