package org.game.yacht.activity

import android.app.AlertDialog
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

    private val exitMain by lazy {
        AlertDialog.Builder(this).setTitle("경고").setMessage("종료하시겠습니까?")
                .setPositiveButton("예") { _, _ -> super.onBackPressed() }
                .setNegativeButton("아니오") { _, _ -> }.create()
    }

//    private val findMatch by lazy {
//        AlertDialog.Builder(this).setTitle("알림").setMessage("상대를 찾았습니다.")
//                .setPositiveButton("예") {_, _ ->
//                    U.send(251)
//                    input.interrupt()
//                    progress.visibility = View.INVISIBLE
//                    connect.isEnabled = true
//                    connect.text = "연결"
//                    startActivity(Intent(this@MainActivity, GameActivity::class.java))
//                }.setNegativeButton("아니오") {_, _ ->
//                    U.send(252)
//                    U.player = -1
//                    status.text = "대결 상대를 찾고 있습니다..."
//                }.create()
//    }

    override fun onBackPressed() = exitMain.show()

    override fun onCreate(savedInstanceState: Bundle?) = with (U) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mHdl = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) = when (msg.what) {
                -1 -> {
                    if (isSocket) close()
                    connect.text = "사용할 별명을 입력하고\n'연결' 버튼을 누르세요."
                    connect.isEnabled = true
                    progress.visibility = View.INVISIBLE
                    connect.text = "연결"
                    toast(this@MainActivity, "오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    isSocket = false
                }
                0, 1 -> {
                    player = 1 + msg.what
//                    findMatch.show()
                    progress.visibility = View.INVISIBLE
                    connect.isEnabled = true
                    connect.text = "연결"
                    startActivity(Intent(this@MainActivity, GameActivity::class.java))
                }

                2 -> {
                    status.text = "서버에 연결 중 입니다..."
                    progress.visibility = View.VISIBLE
                    name.isEnabled = false
                    connect.isEnabled = false
                    connect.text = "대기"
                }
                3 -> status.text = "대결 상대를 찾고 있습니다..."

                else -> toast(this@MainActivity, Error.UNDEFINED_ERROR.ex)
            }
        }

        connect.setOnClickListener {
//            startActivity(Intent(this@MainActivity, GameActivity::class.java))
            if (name.text.isEmpty()) {
                toast(this@MainActivity, "별명을 입력해주세요.")
                return@setOnClickListener
            }
            handle(mHdl, 2)
            Thread {
                try {
                    val socket = Socket("59.12.69.90", 52190)
                    `in` = socket.getInputStream()
                    out = socket.getOutputStream()
                    input.apply { handler = mHdl }.start()
                    isSocket = true

                    out.write(name.text.toString().encodeToByteArray())
                    handle(mHdl, 3)
                } catch (_: Exception) {
                    handle(mHdl, -1)
                }
            }.start()
        }
    }
}