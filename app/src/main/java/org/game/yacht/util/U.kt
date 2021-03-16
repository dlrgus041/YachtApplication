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

    const val YACHT = 16
    const val FOUR_CARD = 8
    const val FULL_HOUSE = 4
    const val L_STRAIGHT = 2
    const val S_STRAIGHT = 1

    lateinit var `in`: InputStream
    lateinit var out: OutputStream

    lateinit var mHdl: Handler
    lateinit var gHdl: Handler
    val input = Receiver().apply { isDaemon = true }

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