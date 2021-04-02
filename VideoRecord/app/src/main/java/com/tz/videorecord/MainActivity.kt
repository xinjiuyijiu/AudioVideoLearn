package com.tz.videorecord

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream

/**
 * @Author:      liuchao
 * @Data:        2021/3/26
 * @Description:
 */
class MainActivity : AppCompatActivity() {

    private lateinit var cameraHandler: Handler
    private val cameraManager by lazy { this.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private lateinit var mediaCodec: MediaCodec
    private var h264FileOutputStream: FileOutputStream? = null
    private var yuvFileOutputStream: FileOutputStream? = null

    companion object {
        private const val TAG = "VideoRecord"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMediaCodec()
        initCamera()
    }


    fun doRecord(view: View) {

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

    private fun initMediaCodec() {
        mediaCodec = MediaCodec.createEncoderByType("video/avc")
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920)
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val h264FilePath = File(getExternalFilesDir(null)!!.absolutePath + "/test_h264_video.h264")
        h264FileOutputStream = FileOutputStream(h264FilePath)
        val yuvFilePath = File(getExternalFilesDir(null)!!.absolutePath + "/test_yuv_video.h264")
        yuvFileOutputStream = FileOutputStream(yuvFilePath)

    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(getBestCamera(), object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                val surface = Surface(textureView.surfaceTexture)
                val captureRequestBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequestBuilder.addTarget(surface)
                val imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.YUV_420_888, 2)
                imageReader.setOnImageAvailableListener({
                    val image = it.acquireNextImage()
                    val planes = image.planes
                    val yByteArray =
                        ByteArray(planes[0].buffer.limit() - planes[0].buffer.position())
                    val uByteArray =
                        ByteArray(planes[1].buffer.limit() - planes[1].buffer.position())
                    val vByteArray =
                        ByteArray(planes[2].buffer.limit() - planes[2].buffer.position())

                    planes[0].buffer.get(yByteArray)
                    planes[1].buffer.get(uByteArray)
                    planes[2].buffer.get(vByteArray)

                    // 将yuv转换成nv12
                    
                    // 将nv12编码成h264
                    val inputBufferIndex = mediaCodec.dequeueInputBuffer(0)
                    if (inputBufferIndex >= 0) {
                        // 申请一个buffer
                        val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        // 填入数据
                        inputBuffer?.put()
                        // 放入MediaCodeC中
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0)
                    }
                    val bufferInfo = MediaCodec.BufferInfo()
                    var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                    var configByteArray = ByteArray(bufferInfo.size)
                    while (outputBufferIndex >= 0) {
                        val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                        val outByteArray = ByteArray(bufferInfo.size)
                        outputBuffer?.get(outByteArray)
                        // 拿到了编码后的数据
                        when (bufferInfo.flags) {
                            MediaCodec.BUFFER_FLAG_CODEC_CONFIG -> {
                                // 获取pps和sps，并保存
                                configByteArray = outByteArray
                            }
                            MediaCodec.BUFFER_FLAG_KEY_FRAME -> {
                                // 关键帧，需要额外添加pps，sps
                                // 文件写入configByteArray 以及 outByteArray
                                h264FileOutputStream?.write(configByteArray)
                                h264FileOutputStream?.write(outByteArray)
                            }
                            else -> {
                                // 文件写入outByteArray
                                h264FileOutputStream?.write(outByteArray)
                            }
                        }

                        // 释放buffer
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                    }


                }, cameraHandler)
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