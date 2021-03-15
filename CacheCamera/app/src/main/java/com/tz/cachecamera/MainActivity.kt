package com.tz.cachecamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val cameraManager: CameraManager by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val handlerThread0 = HandlerThread("cameraThread0")
    private val handlerThread1 = HandlerThread("cameraThread1")
    private lateinit var cameraHandler0: Handler
    private lateinit var cameraHandler1: Handler
    private var preview0SurfaceTexture: SurfaceTexture? = null

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handlerThread0.start()
        handlerThread1.start()
        cameraHandler0 = Handler(handlerThread0.looper)
        cameraHandler1 = Handler(handlerThread1.looper)
        initSurface()
        checkPermission()
    }

    private fun initSurface() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                preview0SurfaceTexture = surface
                preview0SurfaceTexture?.setDefaultBufferSize(1080, 1920)
                openCamera("0")
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

        }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                openCamera("2")
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })
    }

    private fun checkPermission() {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (result != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 200)
        } else {
            initCamera()
        }
    }


    @SuppressLint("MissingPermission")
    private fun initCamera() {
        val caList = cameraManager.cameraIdList
        val sb = StringBuilder()
        for (ca in caList) {
            Log.i(TAG, "initCamera: $ca")
            val cc = cameraManager.getCameraCharacteristics(ca)
            Log.i(
                TAG,
                "initCamera supportFull: ${cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)}"
            )

            Log.i(TAG, "initCamera: direction ${cc.get(CameraCharacteristics.LENS_FACING)}")
            sb.append(
                "camera ID: $ca \n camera support level: ${cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)}\n" +
                        "direction: ${cc.get(CameraCharacteristics.LENS_FACING)}\n"
            )


            val sas = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = sas?.getOutputSizes(SurfaceTexture::class.java)
            for (size in sizes!!) {
                // 支持的像素
                Log.i(TAG, "initCamera: size is  $size")
            }
        }
        cameraInfo.text = sb.toString()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200) {
            initCamera()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String) {
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "onOpened: ${camera.id} is open")

                    val captureRequestBuilder =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    var surface: Surface? = null
                    if (cameraId == "0") {
                        surface = Surface(preview0SurfaceTexture)
                    }

                    if (cameraId == "2") {
                        surface = surfaceView.holder.surface
                    }
                    captureRequestBuilder.addTarget(surface!!)  // 将CaptureRequest的构建器与Surface对象绑定在一起
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )      // 闪光灯
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    ) // 自动对焦

                    val cp = object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            super.onCaptureCompleted(session, request, result)
                            Log.i(TAG, "onCaptureCompleted: ")
                        }
                    }

                    val sessionStateCallback =
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(
                                    captureRequestBuilder.build(), cp,
                                    if (cameraId == "0") cameraHandler0 else cameraHandler1
                                )
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                            }

                        }
                    val outputs = mutableListOf(surface)
                    camera.createCaptureSession(
                        outputs,
                        sessionStateCallback,
                        if (cameraId == "0") cameraHandler0 else cameraHandler1
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.i(TAG, "onOpened: ${camera.id} is disconnected")

                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.i(TAG, "onOpened: ${camera.id} is onError $error")
                }

            },
            if (cameraId == "0") cameraHandler0 else cameraHandler1
        )
    }

    private fun isHardwareLevelSupported(c: CameraCharacteristics, requiredLevel: Int): Boolean {
        val sortedHwLevels = arrayOf(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        )
        val deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (requiredLevel == deviceLevel) {
            return true
        }

        for (sortedlevel in sortedHwLevels) {
            if (sortedlevel == requiredLevel) {
                return true
            } else if (sortedlevel == deviceLevel) {
                return false
            }
        }
        return false
    }

}