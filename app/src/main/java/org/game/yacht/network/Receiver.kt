package org.game.yacht.network

import android.os.Handler
import org.game.yacht.util.U

class Receiver(var handler: Handler? = null): Thread() {
    override fun run() = with (U) {
        try {
            while (true) {
                when (val int = `in`.read()) {
                    -1 -> throw Exception()
                    else -> handle(handler ?: throw Exception(), int)
                }
            }
        } catch (_: Exception) {
            close()
//            handle(handler, 201)
        }
    }
}