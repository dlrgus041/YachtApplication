package org.game.yacht.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.game.yacht.network.Receiver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object U {

    lateinit var `in`: InputStream
    lateinit var out: OutputStream

    lateinit var mHdl: Handler
    lateinit var gHdl: Handler

    val input = Receiver().apply { isDaemon = true }

    var player = -1

    fun random() = 1 + (6 * Math.random()).toInt()

    fun handle(handler: Handler, what: Int, obj: Any? = null) =
        handler.sendMessage(handler.obtainMessage(what, obj))

    fun toast(context: Context, str: String) = Toast.makeText(context, str, Toast.LENGTH_SHORT).show()

    fun send(int: Int) = Thread { out.write(int) }.start()

    fun close() {
        `in`.close()
        out.close()
    }
}