package com.tz.openglstudy

import android.app.ActivityManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val openglVersion = am.deviceConfigurationInfo.glEsVersion.toFloat()
        Log.i(TAG, "onCreate: gl version is   $openglVersion")
        if (openglVersion >= 2.0) {
            gl.setEGLContextClientVersion(2)
            gl.setRenderer(object : GLSurfaceView.Renderer {
                var triangle: Triangle? = null
                val mMVPMatrix = FloatArray(16)
                val mProjectionMatrix = FloatArray(16)
                val mViewMatrix = FloatArray(16)
                val mRotationMatrix = FloatArray(16)

                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                    Log.i(TAG, "onSurfaceCreated: ")
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                    triangle = Triangle()
                }

                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                    Log.i(TAG, "onSurfaceChanged: ")
                    GLES20.glViewport(0, 0, width, height)
                    val ratio = width.toFloat() / height

                    // 这个投影矩阵被应用于对象坐标在onDrawFrame（）方法中
                    Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f);

                }

                override fun onDrawFrame(gl: GL10?) {
                    Log.i(TAG, "onDrawFrame: ")
                    val scratch = FloatArray(16)

                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    // Set the camera position (View matrix)
                    Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

                    // Calculate the projection and view transformation
                    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

                    // 创建一个旋转矩阵
                    val time = SystemClock.uptimeMillis() % 4000L
                    val angle = 0.090f * (time.toInt())
                    Matrix.setRotateM(mRotationMatrix, 0, angle, 0f, 0f, -1.0f)
                    // 将旋转矩阵与投影和相机视图组合在一起
                    // Note that the mMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0)

                    triangle?.draw(scratch)
                }

            })
        }
    }

    override fun onResume() {
        super.onResume()
        gl.onResume()
    }

    override fun onPause() {
        super.onPause()
        gl.onPause()
    }

}