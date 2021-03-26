package com.tz.openglstudy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES20
import androidx.core.content.res.ResourcesCompat
import java.io.ByteArrayOutputStream
import java.nio.*

class Triangle(context: Context) {

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "varying  vec4 vColor;" +
                "varying  vec2 TexCoord;" +
                "attribute  vec2 aTexCoord;" +
                "attribute vec4 aColor;" +
                "void main() {" +
                "  gl_Position =uMVPMatrix * vPosition;" +
                "  vColor=aColor;" +
                "  TexCoord=aTexCoord;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform sampler2D ourTexture;" +
                "varying  vec4 vColor;" +
                "varying  vec2 TexCoord;" +
                "void main() {" +
                "gl_FragColor =texture(ourTexture, TexCoord)* vColor;" +
                "}"


    private var vertexBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    private val color = floatArrayOf(0.0f, 255.0f, 0.0f, 1.0f)
    private val colors = floatArrayOf(
        1.0f, 0f, 0f, 1.0f,
        0f, 1.0f, 0f, 1.0f,
        0f, 0f, 1.0f, 1.0f
    )
    private var program: Int

    companion object {
        val COORDS_PER_VERTEX = 3
        val triangleCoords = floatArrayOf(0.0f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f)
        val texCoords = floatArrayOf(0.0f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f)
        val vertexCount = triangleCoords.size / COORDS_PER_VERTEX
        val vertexStride = COORDS_PER_VERTEX * 4
        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }

    init {
        val byteBuffer = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer?.put(triangleCoords)
        vertexBuffer?.position(0)


        val dd = ByteBuffer.allocateDirect(
            colors.size * 4
        )

        dd.order(ByteOrder.nativeOrder())
        colorBuffer = dd.asFloatBuffer()
        colorBuffer?.put(colors)
        colorBuffer?.position(0)

        val ib = IntBuffer.allocate(1)
        GLES20.glGenTextures(1, ib)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ib.get())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val bitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_background)
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        val buffer = ByteBuffer.wrap(bos.toByteArray())
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGB,
            bitmap.width,
            bitmap.height,
            0,
            GLES20.GL_RGB,
            GLES20.GL_UNSIGNED_BYTE,
            buffer
        )
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bos.close()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
            vertexStride, vertexBuffer
        )

        val texHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(
            texHandle,2, GLES20.GL_FLOAT,false,8,
        )


/*        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        // 设置绘制三角形的颜色
        GLES20.glUniform4fv(colorHandle, 1, color, 0)*/
        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(
            colorHandle, 4, GLES20.GL_FLOAT, false,
            0, colorBuffer
        )

        // 得到形状的变换矩阵的句柄
        val mMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        // 将投影和视图转换传递给着色器
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)

        // GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }


}