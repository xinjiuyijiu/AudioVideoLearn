package com.tz.mp4operation

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    // 提取mp4文件音频和视频信息
    fun extractMp4(view: View) {

        Thread {
            val extractor = MediaExtractor()
            val file = File(Environment.getExternalStorageDirectory(), "boss.mp4")
            val videoFile = File(Environment.getExternalStorageDirectory(), "boss_video.h264")
            val videoFos = FileOutputStream(videoFile)
            val audioFile = File(Environment.getExternalStorageDirectory(), "boss_audio.aac")
            val audioFos = FileOutputStream(audioFile)

            extractor.setDataSource(file.absolutePath)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val mediaFormat = extractor.getTrackFormat(i)
                Log.i(TAG, "extractMp4-----------------------------\n")
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                Log.i(TAG, "extractMp4 keymime: $mime")
                Log.i(
                    TAG,
                    "extractMp4 language: ${mediaFormat.getString(MediaFormat.KEY_LANGUAGE)}"
                )
                try {
                    Log.i(
                        TAG,
                        "extractMp4 width:height: ${mediaFormat.getInteger(MediaFormat.KEY_WIDTH)} : ${
                            mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
                        } "
                    )
                } catch (e: Exception) {
                }
                try {
                    Log.i(
                        TAG,
                        "extractMp4 maxWidth:maxHeight: ${mediaFormat.getInteger(MediaFormat.KEY_MAX_WIDTH)} : ${
                            mediaFormat.getInteger(MediaFormat.KEY_MAX_HEIGHT)
                        } "
                    )
                } catch (e: Exception) {
                }
                Log.i(TAG, "extractMp4 duration: ${mediaFormat.getLong(MediaFormat.KEY_DURATION)}")
                extractor.selectTrack(i)
                val byteBufferSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                val byteBuffer = ByteBuffer.allocate(byteBufferSize)
                var length = 0
                while (extractor.readSampleData(byteBuffer, 0).also { length = it } != -1) {
                    val byteArray = ByteArray(length)
                    byteBuffer.get(byteArray)
                    if (mime!!.startsWith("audio")) {
                        // 如果是aac音频需要添加adts header
                        writeAdtsToStream(audioFos, length + 7)
                        audioFos.write(byteArray)
                    } else {
                        videoFos.write(byteArray)
                    }
                    byteBuffer.clear()
                    extractor.advance()
                }
                if (mime!!.startsWith("audio")) {
                    audioFos.flush()
                    audioFos.close()
                } else {
                    videoFos.flush()
                    videoFos.close()
                }
            }
        }.start()


    }

    private fun writeAdtsToStream(stream: FileOutputStream, frameLength: Int) {
        val profile = 2     // LC
        val freIndex = 4   // 44100hz
        val channel = 2     // 双声道

        val ba = ByteArray(7)
        ba[0] = 0xff.toByte()
        ba[1] = 0xf9.toByte()
        ba[2] = ((profile - 1).shl(8) + freIndex.shl(2) + channel.shr(2)).toByte()
        ba[3] = (channel.and(3).shl(6) + frameLength.shr(11)).toByte()
        ba[4] = frameLength.and(0x7ff).shr(3).toByte()
        ba[5] = (frameLength.and(0x7).shl(5) + 0x1f).toByte()
        ba[6] = 0xFC.toByte()
        stream.write(ba)
    }

    fun mergeMp4(view: View) {
        val filePath = "${Environment.getExternalStorageDirectory()}/merge_boss.mp4"
        val mediaMuxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2)
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 960, 544)
        val audioTrackIndex = mediaMuxer.addTrack(audioFormat)
        val videoTrackIndex = mediaMuxer.addTrack(videoFormat)
        mediaMuxer.start()
        // 写入audioTrackIndex
        val byteBuffer = ByteBuffer.allocate(20)

        val audioBufferInfo = MediaCodec.BufferInfo()
        mediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, audioBufferInfo)

        // 写入videoTrackIndex
        mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, audioBufferInfo)

        mediaMuxer.stop()
        mediaMuxer.release()
    }
}