package com.tz.openglstudy

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Triangle {

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "varying  vec4 vColor;" +
                "attribute vec4 aColor;" +
                "void main() {" +
                "  gl_Position =uMVPMatrix * vPosition;" +
                "  vColor=aColor;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "varying  vec4 vColor;" +
                "void main() {" +
                "gl_FragColor = vColor;" +
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


        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }


}