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

    private val scoreboard by lazy { findViewById<LinearLayout>(R.id.scoreboard) }
    private val score1 by lazy { findViewById<TableLayout>(R.id.score1) }
    private val score2 by lazy { findViewById<TableLayout>(R.id.score2) }
//    private val notice by lazy { findViewById<TextView>(R.id.notice) }
    private val fixed by lazy { findViewById<LinearLayout>(R.id.fixed) }
    private val roll by lazy { findViewById<LinearLayout>(R.id.roll) }
    private val btnScore by lazy { findViewById<Button>(R.id.btnScore) }
    private val btnConfirm by lazy { findViewById<Button>(R.id.btnConfirm) }
    private val time by lazy { findViewById<CircleProgressBar>(R.id.time) }

    private val dice = Array(5) { 0 }
    private val img = Array(7) { 0 }

    private var isMyTurn = false
    private var isThrowable = false
    private var isScore = false
    private var remain = 3

    private fun initialize(first: Boolean = false) {
        for (i in 0 .. 4) {
            with (roll[i] as ImageView) {
                if (visibility == View.VISIBLE) {
                    setImageResource(img[0])
                    if (!first) U.send(10 * i)
                }
            }
        }
    }

    private fun shuffle(first: Boolean = false) {
        for (i in 0 .. 4) {
            if (roll[i].visibility == View.VISIBLE) {
                dice[i] = U.random()
                if (!first) U.send(10 * i + dice[i])
            }
        }
    }

    private fun display(index: Int, num: Int) {
        if (num < 7) dice[index] = num
        (roll[index] as ImageView).setImageResource(img[dice[index]])
    }

    private fun showAll() = Thread {
        for (i in 0 .. 4) {
            U.handle(U.gHdl, 10 * i + 7)
            Thread.sleep(300)
        }
    }.start()

    private fun setDice(fix :Boolean, index: Int) {
        if (fix) {
            roll[index].visibility = View.GONE
            with (fixed[index] as ImageView) {
                setImageResource(img[dice[index]])
                visibility = View.VISIBLE
            }
        } else {
            roll[index].visibility = View.VISIBLE
            fixed[index].visibility = View.GONE
        }
    }

    private fun setBtn(`throw`: Boolean, wait: Boolean = false) = with (btnConfirm) {
        if (`throw`) {
//            notice.text = if (wait) "주사위 눈의 수의 합이\n더 큰 사람이 선공을 가져갑니다."
//            else "주사위를 굴려서 조합을 만드세요!\n남은 기회 : ${remain}회"
            text = "굴린다!"
            isEnabled = true
        } else {
            if (wait) {
//                notice.text = "상대를 기다리는 중..."
                text = "대기중"
                isEnabled = false
            } else {
//                notice.text = "보관할 주사위와 굴릴 주사위를 선택하세요."
                text = "선택 완료"
            }
        }
    }

    private fun waitResponse(first: Boolean = false) = U.send(if (first) 250 else 255)

//    override fun onDestroy() {
//        with (U) {
//            send(251)
//            close()
//        }
//        super.onDestroy()
//    }

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
                in 0 .. 49 -> {
                    display(msg.what / 10, (msg.what % 10))
                }
                in 50 .. 59 -> { // receive opponent dice
                    val index = msg.what - 50
                    if (index < 5) {
                        setDice(false, index)
                    } else {
                        setDice(true, index - 5)
                    }
                }
                65 -> U.toast(this@GameActivity, "후공입니다.")
                69 -> {
                    U.toast(this@GameActivity, "선공입니다.")
                    isMyTurn = true
                    isThrowable = true
                    initialize()
                    setBtn(true)
                }
                250 -> setBtn(true, wait = true)
                255 -> { // notify that my turn is done
                    setBtn(true, wait = true)
                }
                else -> TODO("error dialog")
            }
        }

        for (i in 0 .. 4) {
            fixed[i].visibility = View.GONE
            roll[i].setOnClickListener {
                if (isMyTurn && !isThrowable && (it.visibility == View.VISIBLE)) {
                    U.send(55 + i)
                    setDice(true, i)
                }
            }
            fixed[i].setOnClickListener {
                if (isMyTurn && !isThrowable && (it.visibility == View.VISIBLE)) {
                    U.send(50 + i)
                    setDice(false, i)
                }
            }
        }

        U.receive()
        time.max = 60
        setBtn(false, wait = true)
        initialize(first = true)
        waitResponse(first = true)

        with (btnScore) {
            setOnClickListener {
                scoreboard.visibility = if (isScore) {
                    isEnabled = false
                    View.INVISIBLE
                } else {
                    isEnabled = true
                    View.VISIBLE
                }
                isScore = !isScore
            }
        }

        btnConfirm.setOnClickListener {
            if (!isMyTurn && btnConfirm.isEnabled) {
                shuffle(first = true)
                showAll()
                setBtn(false, wait = true)
                U.send(200 + dice.sum())
            } else {
                if (isThrowable) {
                    shuffle()
                    setBtn(false)
                } else {
                    if (--remain > 0) {
                        setBtn(true)
                    } else {
                        setBtn(false, wait = false)
                        isMyTurn = false
                        waitResponse()
                    }
                }
                isThrowable = !isThrowable
            }
        }
    }
}