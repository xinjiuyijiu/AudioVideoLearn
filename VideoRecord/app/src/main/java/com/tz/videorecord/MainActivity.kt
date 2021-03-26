package com.tz.videorecord

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Camera
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.SessionConfiguration
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @Author:      liuchao
 * @Data:        2021/3/26
 * @Description:
 */
class MainActivity : AppCompatActivity() {

    private lateinit var cameraHandler: Handler
    private val cameraManager by lazy { this.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    companion object {
        private const val TAG = "VideoRecord"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initCamera()
    }

    private fun initCamera() {
        val ht = HandlerThread("camera-thread")
        ht.start()
        cameraHandler = Handler(ht.looper)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
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
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(getBestCamera(), object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                val surface = Surface(textureView.surfaceTexture)
                val captureRequestBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequestBuilder.addTarget(surface)
                val outputSurfaces = mutableListOf(surface)
                camera.createCaptureSession(
                    outputSurfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                object : CameraCaptureSession.CaptureCallback() {

                                },
                                cameraHandler
                            )
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {

                        }

                    },
                    cameraHandler
                )
            }

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }

        }, cameraHandler)
    }

    /**
     * 获取REQUEST_AVAILABLE_CAPABILITIES最多的逻辑摄像头
     */
    private fun getBestCamera(): String {
        var cameraId = ""
        var maxSize = 0
        cameraManager.cameraIdList.forEach {
            val cmm = cameraManager.getCameraCharacteristics(it)
            val abilities = cmm.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            Log.i(TAG, "getBestCamera:  cameraId $it  abilities ${abilities.contentToString()}")
            if (maxSize < abilities!!.size) {
                maxSize = abilities.size
                cameraId = it
            }
        }
        return cameraId
    }
}