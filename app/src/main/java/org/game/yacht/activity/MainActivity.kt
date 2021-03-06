package org.game.yacht.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.game.yacht.R
import org.game.yacht.util.Error
import org.game.yacht.util.U
import java.net.Socket

class MainActivity: AppCompatActivity() {

    private val status by lazy { findViewById<TextView>(R.id.status) }
    private val name by lazy { findViewById<EditText>(R.id.name) }
    private val connect by lazy {findViewById<Button>(R.id.connect) }
    private val progress by lazy { findViewById<ProgressBar>(R.id.progress) }
    private var isSocket = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        U.mHdl = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) = when (msg.what) {
                -1 -> {
                    if (isSocket) U.close()
                    connect.isEnabled = true
                    progress.visibility = View.INVISIBLE
                    connect.text = "연결"
                    U.toast(this@MainActivity, "오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    isSocket = false
                }
                0 -> {
                    status.text = "서버에 연결 중 입니다..."
                    progress.visibility = View.VISIBLE
                }
                1 -> {
                    U.find()
                    status.text = "대결 상대를 찾고 있습니다..."
                    connect.isEnabled = false
                    connect.text = "대기"
                }
                2 -> {
                    progress.visibility = View.INVISIBLE
                    startActivity(Intent(this@MainActivity, GameActivity::class.java))
                }

                else -> U.toast(this@MainActivity, Error.UNDEFINED_ERROR.ex)
            }
        }

        connect.setOnClickListener {
//            startActivity(Intent(this@MainActivity, GameActivity::class.java))
            if (name.text.isEmpty()) {
                U.toast(this, "별명을 입력해주세요.")
                return@setOnClickListener
            }
            U.handle(U.mHdl, 0)
            Thread {
                try {
                    val socket = Socket("59.12.69.90", 52196)
                    U.`in` = socket.getInputStream()
                    U.out = socket.getOutputStream()
                    isSocket = true

                    U.out.write(name.text.toString().encodeToByteArray())
                    U.handle(U.mHdl, 1)
                } catch (_: Exception) {
                    U.handle(U.mHdl, -1)
                }
            }.start()
        }
    }
}