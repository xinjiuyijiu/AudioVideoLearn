package com.tz.audiopcm

import org.junit.Test

import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @ExperimentalUnsignedTypes
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val a = "RIFF"
        val bb = a.encodeToByteArray()
        for (b in bb) {
            println("bb is ${b.toString(16)}")
        }
        val os = ByteArrayOutputStream()
        os.write("RIFF".encodeToByteArray())



        val i = Int.MAX_VALUE
        val result = ByteArray(4)
        result[0] = i.ushr(24).and(0xFF).toByte()
        result[1] = i.ushr(16).and(0xFF).toByte()
        result[2] = i.ushr(8).and(0xFF).toByte()
        result[3] = i.and(0xFF).toByte()

        result.reverse()

        for (re in result) {
            println("result is ${re.toString(16)}")
        }


        println("88200 int to byte is ${88200.toUByte()}")
        println("88200 int to byte is ${88200.and(0xFF).toByte()}")
    }
}