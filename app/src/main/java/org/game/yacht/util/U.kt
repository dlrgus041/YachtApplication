package org.game.yacht.util

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

object U {
    lateinit var `in`: InputStream
    lateinit var out: OutputStream

    lateinit var mHdl: Handler
    lateinit var gHdl: Handler

    private val erHdl = object: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) = TODO("error dialog")
    }

    val score = Array(2) {Array(12) {0} }

    fun random() = (1 + 6 * Math.random()).toInt().toByte()

//    fun dialog(context: Context, title: String, message: String, error: Boolean): AlertDialog {
//        val ret = AlertDialog.Builder(context).setTitle(title).setMessage(message)
//        if (error) {
//            ret.setNeutralButton("확인") {_, _ -> }
//        }
//    }

    fun handle(handler: Handler, what: Int, obj: Any? = null) =
        handler.sendMessage(handler.obtainMessage(what, obj))

    fun toast(context: Context, str: String) = Toast.makeText(context, str, Toast.LENGTH_SHORT).show()

    fun find() = Thread {
        try {
            when (`in`.read()) {
                0 -> handle(mHdl, 2)
                else -> handle(mHdl, -1)
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
                    -1 -> throw EOFException()
                    in 50 .. 59 -> handle(gHdl, 50, int - 50)
                    else -> handle(gHdl, int)
                }
            }
        } catch (_: IOException) {
            handle(erHdl, 1)
        }
    }.start()

    fun close() {
        `in`.close()
        out.close()
    }
}