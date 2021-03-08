package org.game.yacht.util

import android.content.Context
import android.os.Handler
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object U {

    lateinit var `in`: InputStream
    lateinit var out: OutputStream

    lateinit var mHdl: Handler
    lateinit var gHdl: Handler

    var player = -1

    fun random() = 1 + (6 * Math.random()).toInt()

    fun handle(handler: Handler, what: Int, obj: Any? = null) =
        handler.sendMessage(handler.obtainMessage(what, obj))

    fun toast(context: Context, str: String) = Toast.makeText(context, str, Toast.LENGTH_SHORT).show()

    fun find() = Thread {
        try {
            when (val int = `in`.read()) {
                -1 -> handle(mHdl, -1)
                2, 3 -> handle(mHdl, int)
                else -> throw IOException()
            }
        } catch (_: Exception) {
            handle(mHdl, -1)
        }
    }.start()

    fun send(int: Int) = Thread { out.write(int) }.start()

    fun receive() = Thread {
        try {
            while (true) {
                when (val int = `in`.read()) {
                    -1 -> throw IOException()
                    else -> handle(gHdl, int)
                }
            }
        } catch (_: IOException) {
            close()
            handle(gHdl, 201)
        }
    }.start()

    fun close() {
        `in`.close()
        out.close()
    }
}