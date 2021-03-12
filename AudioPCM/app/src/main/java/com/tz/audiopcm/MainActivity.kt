package com.tz.audiopcm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.tz.audiopcm.R.layout.activity_main
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack

    // 0 stop  1 playing
    private var recordState: Int = 0
    private var bufferSize: Int = 0
    private var atBufferSize: Int = 0
    private var threadPool: ExecutorService? = null
    private var fileName: String = ""
    private var fos: FileOutputStream? = null


    companion object {
        val tformat = SimpleDateFormat("HHmmss")
        val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_main)
        verifyAudioPermissions()
        initRecord()
    }

    private fun initRecord() {
        bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        atBufferSize = AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            atBufferSize,
            AudioTrack.MODE_STREAM
        )

        threadPool = Executors.newFixedThreadPool(1)
    }

    private fun readData() {
        fileName = "${getExternalFilesDir(null)}/test_record_pcm_${tformat.format(Date())}.pcm"
        Log.i(TAG, "fileName: ${fileName}")
        val file = File(fileName)
        fos = FileOutputStream(file)
        val bufferData = ByteArray(bufferSize)
        while (recordState == 1) {
            val readSize = audioRecord.read(bufferData, 0, bufferSize)
            if (readSize >= 0) {
                // 读取实时录音数据
                fos?.write(bufferData)
            }
        }
        fos?.close()
    }

    fun doRecord(view: View) {
        if (recordState == 1) {
            recordState = 0
            audioRecord.stop()
            record.text = "录制暂停"
        } else {
            record.text = "正在录制"
            recordState = 1
            audioRecord.startRecording()
            // 启动线程读取数据
            threadPool?.execute {
                readData()
            }
        }
    }

    fun stopRecord(view: View) {
        recordState = 0
        audioRecord.release()
        pcmToWav(fileName)
    }

    fun playRecord(view: View) {
        threadPool?.execute {
            playData()
        }
    }

    private fun playData() {
        val file = File(fileName)
        val fis = FileInputStream(file)
        val bufferData = ByteArray(atBufferSize)
        audioTrack.play()
        var len: Int
        while ((fis.read(bufferData, 0, atBufferSize).also { len = it }) != -1) {
            audioTrack.write(bufferData, 0, len)
        }
        fis.close()
    }


    /*
   * 申请录音权限*/
    fun verifyAudioPermissions() {
        val permission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 101
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pcmToWav(pcmFilePath: String) {
        threadPool?.execute {
            val pcmFile = File(pcmFilePath)
            val wavFilePath = "${pcmFile.parent}/wav_${pcmFile.name}"
            val wavFile = File(wavFilePath)
            wavFile.createNewFile()
            pcmFile.copyTo(wavFile, true)
            // todo 文件可能超过 2GB导致Int溢出
            val fileLength = pcmFile.length().toInt()
            // 构建wav前72个字节
            val os = RandomAccessFile(File(wavFilePath), "rws")
            os.seek(0)

            os.write("RIFF".encodeToByteArray())
            os.write(intToByteArray(fileLength + 44 - 8))// 文件大小
            os.write("WAVE".encodeToByteArray())

            os.write("fmt ".encodeToByteArray())
            os.write(intToByteArray(16))
            os.write(shortToByteArray(1))
            os.write(shortToByteArray(1))
            os.write(intToByteArray(44100))
            os.write(intToByteArray(44100 * 1 * 16 / 2))
            os.write(shortToByteArray(1 * 16 / 2))
            os.write(shortToByteArray(16))

            os.write("data".encodeToByteArray())
            os.write(intToByteArray(fileLength))

            os.close()

        }
    }

    private fun intToByteArray(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = i.shr(24).and(0xff).toByte()
        result[1] = i.shr(16).and(0xff).toByte()
        result[2] = i.shr(8).and(0xff).toByte()
        result[3] = i.and(0xff).toByte()
        result.reverse()
        return result
    }

    private fun shortToByteArray(i: Short): ByteArray {
        val result = ByteArray(2)
        result[0] = i.toInt().shr(8).and(0xff).toByte()
        result[1] = i.toInt().and(0xff).toByte()
        result.reverse()
        return result
    }


}