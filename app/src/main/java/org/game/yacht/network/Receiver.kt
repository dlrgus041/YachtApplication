package org.game.yacht.network

import android.os.Handler
import org.game.yacht.util.U

class Receiver(var handler: Handler? = null): Thread() {
    override fun run() = with (U) {
        try {
            while (true) {
                when (val code = `in`.read()) {
                    -1 -> throw Exception()
                     101, 102 -> {
                        val byteArr = ByteArray(1024)
                        when (val len = `in`.read(byteArr)) {
                            -1 -> throw Exception()
                            else -> handle(handler ?: throw Exception(), code, byteArr.decodeToString(0, len))
                        }
                    }
                    else -> handle(handler ?: throw Exception(), code)
                }
            }
        } catch (_: Exception) {
            close()
//            handle(handler, 201)
        }
    }
}