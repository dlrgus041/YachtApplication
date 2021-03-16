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
    private val quickStart by lazy {findViewById<Button>(R.id.quickStart) }
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) = with (U) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mHdl = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) = when (msg.what) {
                -1 -> {
                    if (isSocket) close()
                    quickStart.text = "사용할 별명을 입력하고\n'연결' 버튼을 누르세요."
                    quickStart.isEnabled = true
                    progress.visibility = View.INVISIBLE
                    quickStart.text = "연결"
                    toast(this@MainActivity, "mHdl - (-1)")
                    isSocket = false
                }
                101, 102 -> {
//                    findMatch.show()
                    progress.visibility = View.INVISIBLE
                    quickStart.isEnabled = true
                    quickStart.text = "연결"
                    startActivity(Intent(this@MainActivity, GameActivity::class.java)
                            .apply {
                                putExtra("myName", name.text.toString())
                                putExtra("opName", msg.obj as String)
                                putExtra("player", msg.what - 100)
                            }
                    )
                }
                103 -> {
                    status.text = "서버에 연결 중 입니다..."
                    progress.visibility = View.VISIBLE
                    name.isEnabled = false
                    quickStart.isEnabled = false
                    quickStart.text = "대기"
                }
                104 -> status.text = "대결 상대를 찾고 있습니다..."

                else -> toast(this@MainActivity, "mHdl - 정의되지 않은 코드 (${msg.what})")
            }
        }

        quickStart.setOnClickListener {
//            startActivity(Intent(this@MainActivity, GameActivity::class.java))
            if (name.text.isEmpty()) {
                toast(this@MainActivity, "별명을 입력해주세요.")
                return@setOnClickListener
            }
            handle(mHdl, 103)
            Thread {
                try {
                    val socket = Socket("59.12.69.90", 52190)
                    `in` = socket.getInputStream()
                    out = socket.getOutputStream()
                    input.apply { handler = mHdl }.start()
                    isSocket = true

                    out.write(name.text.toString().encodeToByteArray())
                    handle(mHdl, 104)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }
}