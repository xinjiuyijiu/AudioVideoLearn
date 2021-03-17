package com.tz.cachecamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.*
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
        textureView.postDelayed({
            openMultiCamera()
        }, 2000)
    }

    private fun initSurface() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

                preview0SurfaceTexture = surface
                preview0SurfaceTexture?.setDefaultBufferSize(720, 1280)
                //openCamera("0")
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
                // openCamera("1")
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

        initTextureView(surfaceView0)
        initTextureView(surfaceView1)
        initTextureView(surfaceView2)
        initTextureView(surfaceView3)

    }

    val surfaceList = mutableListOf<SurfaceTexture>()

    private fun initTextureView(tx: TextureView) {
        tx.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surfaceList.add(surface)
                surface.setDefaultBufferSize(720, 1280)
                //openCamera("0")
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
            val rac = cc.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            sb.append(
                "camera ID: $ca \n camera support level: ${cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)}\n" +
                        "direction: ${if (cc.get(CameraCharacteristics.LENS_FACING) == 0) "front" else "back"}\n" +
                        "capabilities:  ${rac.contentToString()}\n" +
                        "physical camera: ${
                            cc.physicalCameraIds.toTypedArray().contentToString()
                        }\n" +
                        "\n\n"
            )

            // CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            Log.i(TAG, "initCamera: $rac")
            rac?.forEach {
                Log.i(TAG, "initCamera: rac is $it")
            }

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

    // ir 如果是local field 会导致 buffer abandon的错误，因为会被回收
    val ir = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String) {
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "onOpened: ${camera.id} is open")

                    val captureRequestBuilder =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                    val outputs = mutableListOf<Surface?>()


                    var surface0: Surface? = null
                    if (cameraId == "0") {
                        surface0 = Surface(preview0SurfaceTexture)
                        captureRequestBuilder.addTarget(surface0!!)  // 将CaptureRequest的构建器与Surface对象绑定在一起
                        outputs.add(surface0)
                    }


                    var surface1: Surface? = null
                    if (cameraId == "1") {
                        surface1 = surfaceView.holder.surface
                        captureRequestBuilder.addTarget(surface1!!)  // 将CaptureRequest的构建器与Surface对象绑定在一起
                        outputs.add(surface1)
                        // 添加一个ImageReader
                        ir.setOnImageAvailableListener({
                            // 这里捕获的每一帧数据，即时处理
                            val image = it.acquireNextImage()
                            val buffer = image.planes[0].buffer
                            val pixels = ByteArray(buffer.remaining())
                            buffer.get(pixels)
                            image.close()

                            val bitmap = BitmapFactory.decodeByteArray(pixels, 0, pixels.size)
                            runOnUiThread {
                                if (bitmap != null) {
                                    this@MainActivity.image.setImageBitmap(bitmap)
                                }
                            }

                        }, cameraHandler0)

                        captureRequestBuilder.addTarget(ir.surface!!)
                        outputs.add(ir.surface)
                    }


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


    @SuppressLint("MissingPermission")
    private fun openMultiCamera() {
        cameraManager.openCamera("0", object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {


                 val suface0 = Surface(surfaceList[0])
                 val suface1 = Surface(surfaceList[1])
                 val suface2 = Surface(surfaceList[2])
                 val suface3 = Surface(surfaceList[3])
                val physicalConfig1 = OutputConfiguration(suface0)
                physicalConfig1.setPhysicalCameraId("2")
                val physicalConfig2 = OutputConfiguration(suface1)
                physicalConfig2.setPhysicalCameraId("3")
                val physicalConfig3 = OutputConfiguration(suface2)
                physicalConfig3.setPhysicalCameraId("3")
                val physicalConfig4 = OutputConfiguration(suface3)
                physicalConfig4.setPhysicalCameraId("3")
                val outputs = mutableListOf(
                    physicalConfig1,
                    physicalConfig2,
                    physicalConfig3,
                    physicalConfig4
                )

                val mPreViewBuidler =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                mPreViewBuidler.addTarget(suface0)
                mPreViewBuidler.addTarget(suface1)
                mPreViewBuidler.addTarget(suface2)
                mPreViewBuidler.addTarget(suface3)
//                mPreViewBuidler.set(
//                    CaptureRequest.CONTROL_AE_MODE,
//                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
//                )      // 闪光灯
//                mPreViewBuidler.set(
//                    CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//                ) // 自动对焦

                val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    AsyncTask.SERIAL_EXECUTOR,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {

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

                            session.setRepeatingRequest(
                                mPreViewBuidler.build(),
                                null,
                                cameraHandler0
                            )
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {

                        }
                    })
                camera.createCaptureSession(config)
            }

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }

        }, cameraHandler0)
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