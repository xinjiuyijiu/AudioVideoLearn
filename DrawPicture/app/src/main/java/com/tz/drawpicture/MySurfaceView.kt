package com.tz.drawpicture

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

class MySurfaceView : SurfaceView, SurfaceHolder.Callback {

    private var mPaint: Paint? = null
    private var mCanvas: Canvas? = null
    private var mBitmap: Bitmap

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context?, attributeSet: AttributeSet, defStyle: Int) : super(
        context,
        attributeSet,
        -1
    )

    companion object {
        val TAG = "MySurfaceView"
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
        mPaint = Paint()
        mPaint?.color = Color.GREEN
        mPaint?.flags = Paint.ANTI_ALIAS_FLAG
        mPaint?.style = Paint.Style.STROKE
        val op = BitmapFactory.Options()
        op.inJustDecodeBounds = true
        BitmapFactory.decodeResource(context.resources, R.drawable.live_ad_5, op)
        val realWidth = resources.displayMetrics.widthPixels
        val realHeight = op.outHeight * realWidth / op.outWidth
        mBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.live_ad_5)
        mBitmap = Bitmap.createScaledBitmap(mBitmap, realWidth, realHeight, true)

    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        Thread {
            mCanvas = holder.lockCanvas()
            mCanvas!!.drawBitmap(mBitmap, 0f, 0f, mPaint)
            holder.unlockCanvasAndPost(mCanvas)
        }.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }
}