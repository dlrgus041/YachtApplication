package org.game.yacht.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.iterator
import com.dinuscxj.progressbar.CircleProgressBar
import org.game.yacht.R
import org.game.yacht.util.U
import kotlin.concurrent.timer

class GameActivity: AppCompatActivity() {

    private val gs by lazy { findViewById<TableLayout>(R.id.gs)}
    private val notice by lazy { findViewById<TextView>(R.id.notice) }
    private val fixed by lazy { findViewById<LinearLayout>(R.id.fixed) }
    private val roll by lazy { findViewById<LinearLayout>(R.id.roll) }
    private val btn by lazy { findViewById<Button>(R.id.btn) }
    private val time by lazy { findViewById<CircleProgressBar>(R.id.time) }

    private val dice = ByteArray(5)
    private val img = Array(7) { 0 }

    private var isMyTurn = false
    private var isThrowable = false
    private var remain = 3

    private fun initialize() { for (view in roll) (view as ImageView).setImageResource(img[0]) }

    private fun shuffle(first: Boolean = false) {
        for (i in 0 .. 4) {
            dice[i] = U.random()
            if (!first) U.send(10 * i + dice[i])
        }
    }

    private fun display(index: Int, num: Byte = 0) {
        if (num > 0) dice[index] = num
        (roll[index] as ImageView).setImageResource(img[dice[index].toInt()])
    }

    private fun showAll() = Thread {
        for (i in 0 .. 4) {
            display(i)
            if (roll[i].visibility == View.VISIBLE) Thread.sleep(300)
        }
    }.start()

    private fun setDice(fix :Boolean, index: Int) {
        if (fix) {
            roll[index].visibility = View.GONE
            fixed[index].visibility = View.VISIBLE
        } else {
            roll[index].visibility = View.VISIBLE
            fixed[index].visibility = View.GONE
        }
    }

    private fun setBtn(`throw`: Boolean, wait: Boolean = false) = with (btn) {
        if (`throw`) {
            notice.text = if (wait) "주사위 눈의 수의 합이\n더 큰 사람이 선공을 가져갑니다."
            else "주사위를 굴려서 조합을 만드세요!\n남은 기회 : ${--remain}회"
            text = "굴린다!"
            isEnabled = true
        } else {
            if (wait) {
                notice.text = "상대를 기다리는 중..."
                text = "대기중"
                isEnabled = false
            } else {
                notice.text = "보관할 주사위와 굴릴 주사위를 선택하세요."
                text = "선택 완료"
            }
        }
    }

    private fun waitResponse() = U.send(255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        setContentView(R.layout.activity_game)

        for (i in 0 .. 6)
            img[i] = resources.getIdentifier("dice$i", "drawable", packageName)

        U.gHdl = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message): Unit = when (msg.what) {
                in 1 .. 46 -> display(msg.what / 10, (msg.what % 10).toByte())
                50 -> {
                    when (msg.obj as Int) {
                        0 -> { // timer countdown
                        }
                        1 -> {
                            U.toast(this@GameActivity, "후공입니다.")
                        }
                        9 -> {
                            U.toast(this@GameActivity, "선공입니다.")
                            isMyTurn = true
                            isThrowable = true
                            setBtn(true)
                        }
                        else -> {
                        }
                    }
                }
                in 100 .. 110 -> { // receive opponent dice
                    val index = msg.what - 100
                    if (index < 5) {
                        setDice(false, index)
                    } else {
                        setDice(true, index - 5)
                    }
                }
                200 -> { // decide
                    setBtn(false)
                }
                255 -> { // confirm that player is ready
                    setBtn(true, wait = true)
                }
                else -> TODO("error dialog")
            }
        }

        for (i in 0 .. 4) {
            fixed[i].visibility = View.INVISIBLE
            roll[i].setOnClickListener {
                if (isMyTurn && !isThrowable && (it.visibility == View.VISIBLE)) {
                    U.send(105 + i)
                    setDice(true, i)
                }
            }
            fixed[i].setOnClickListener {
                if (isMyTurn && !isThrowable && (it.visibility == View.VISIBLE)) {
                    U.send(100 + i)
                    setDice(false, i)
                }
            }
        }

        U.receive()
        time.max = 60
        setBtn(false, wait = true)
        initialize()
        waitResponse()

        btn.setOnClickListener {
            if (!isMyTurn && btn.isEnabled) {
                shuffle(first = true)
                showAll()
                setBtn(false, wait = true)
                U.send(dice.sum())
            } else {
                if (isThrowable) {
                    shuffle()
                    setBtn(false)
                } else {
                    if (remain > 0) {
                        setBtn(true)
                    } else {
                        setBtn(false, wait = false)
                        isMyTurn = false
                        waitResponse()
                    }
                }
            }
        }
    }
}