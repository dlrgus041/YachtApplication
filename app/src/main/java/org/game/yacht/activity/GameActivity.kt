package org.game.yacht.activity

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.dinuscxj.progressbar.CircleProgressBar
import org.game.yacht.R
import org.game.yacht.util.U

const val YACHT = 16
const val FOUR_CARD = 8
const val FULL_HOUSE = 4
const val L_STRAIGHT = 2
const val S_STRAIGHT = 1

class GameActivity: AppCompatActivity() {

    private val scoreboard by lazy { findViewById<LinearLayout>(R.id.scoreboard) }
    private val score1 by lazy { findViewById<TableLayout>(R.id.score1) }
    private val score2 by lazy { findViewById<TableLayout>(R.id.score2) }
    private val notice by lazy { findViewById<TextView>(R.id.notice) }
    private val fixed by lazy { findViewById<LinearLayout>(R.id.fixed) }
    private val roll by lazy { findViewById<LinearLayout>(R.id.roll) }
    private val btnScore by lazy { findViewById<Button>(R.id.btnScore) }
    private val btnConfirm by lazy { findViewById<Button>(R.id.btnConfirm) }
    private val time by lazy { findViewById<CircleProgressBar>(R.id.time) }

    private val exitGame by lazy {
        AlertDialog.Builder(this).setTitle("경고").setMessage("자동 패배 처리 됩니다. 게속 하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    U.send(200)
                    U.close()
                    super.onBackPressed()
                }
                .setNegativeButton("아니오") { _, _ -> }.create()
    }

    private val img = Array(7) { 0 }
    private val dice = Array(5) { 0 }
    private val cnt = Array(7) { 0 }

    private var isMyTurn = false
    private var isThrowable = false
    private var remain = 3

    private fun setScore(table: TableLayout, category: Int, value: Int, opponent: Boolean = false) {
        ((table[if (opponent) (U.player + 1) % 2 else U.player] as TableRow)[category] as TextView).hint = value.toString()
    }

    private fun judge(): Int {
        var ret = 0
        for (i in 1 .. 6) cnt[i] = 0
        for (i in 0 .. 4) cnt[dice[i]]++

        if (cnt[2] * cnt[3] * cnt[4] * cnt[5] == 1 && (cnt[1] == 1 || cnt[6] == 1))
            ret = ret or L_STRAIGHT
        if (cnt[3] * cnt[4] > 0 && (cnt[1] * cnt[2] > 0 || cnt[2] * cnt[5] > 0 || cnt[5] * cnt[6] > 0))
            ret = ret or S_STRAIGHT

        cnt.sortDescending()
        if (cnt[0] == 5) ret = ret or YACHT or FULL_HOUSE
        if (cnt[0] == 4) ret = ret or FOUR_CARD
        if (cnt[0] == 3 && cnt[1] == 2) ret = ret or FULL_HOUSE

        return ret
    }

    private fun calculate(opponent: Boolean = false) {
        with(score1) {
            for (i in 1..6) setScore(this, i, dice.filter { it == i }.sum(), opponent)
        }

        val mask = judge()
        with(score2) {
            setScore(this, 1, dice.sum(), opponent)
            setScore(this, 2, if (mask and FOUR_CARD != 0) dice.sum() else 0, opponent)
            setScore(this, 3, if (mask and FULL_HOUSE != 0) dice.sum() else 0, opponent)
            setScore(this, 4, if (mask and S_STRAIGHT != 0) 15 else 0, opponent)
            setScore(this, 5, if (mask and L_STRAIGHT != 0) 30 else 0, opponent)
            setScore(this, 6, if (mask and YACHT != 0) dice.sum() else 0, opponent)
        }
    }

    private fun clearDice(first: Boolean = false) {
        if (first) btnScore.isEnabled = true
        for (i in 0 .. 4) {
            with (roll[i] as ImageView) {
                if (visibility == View.VISIBLE) setImageResource(img[0])
            }
        }
    }

    private fun shuffle() {
        for (i in 0 .. 4) {
            if (roll[i].visibility == View.VISIBLE) {
                dice[i] = U.random()
            }
        }
    }

    private fun display(index: Int, num: Int) {
        if (num < 7) dice[index] = num
        (roll[index] as ImageView).setImageResource(img[dice[index]])
    }

    private fun showAll(first: Boolean = false) = Thread {
        for (i in 0 .. 4) {
            if (!first) U.send(10 * i + dice[i])
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
            notice.text = if (wait) "주사위 눈의 수의 합이 더 큰 사람이 선공입니다."
            else "주사위를 굴려서 조합을 만드세요."
            text = "굴린다!"
            isEnabled = true
        } else {
            if (wait) {
                notice.text = "상대를 기다리는 중..."
                text = "대기중"
                isEnabled = false
                U.send(70)
            } else {
                notice.text = "남은 기회 : ${remain}회"
                text = "선택 완료"
            }
        }
    }

    private fun errAlert(message: String) = AlertDialog.Builder(this).setTitle("알림")
            .setMessage(message).setNeutralButton("확인") { _, _ -> }.create()

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
                in 0 .. 49 -> display(msg.what / 10, (msg.what % 10))
                in 50 .. 59 -> { // receive opponent dice
                    val index = msg.what - 50
                    if (index < 5) {
                        setDice(false, index)
                    } else {
                        setDice(true, index - 5)
                    }
                }
                65 -> {
                    U.toast(this@GameActivity, "후공입니다.")
                    clearDice(first = true)
                }
                69 -> {
                    U.toast(this@GameActivity, "선공입니다.")
                    isMyTurn = true
                    isThrowable = true
                    clearDice(first = true)
                    setBtn(true)
                }
                70 -> { // inform that my turn is ready
                    if (btnScore.isEnabled) {
                        if (isMyTurn) {
                            notice.text = "상대의 차례입니다."
                        } else {
                            remain = 3
                            isThrowable = true
                            setBtn(true)
                        }
                        isMyTurn = !isMyTurn
                    } else setBtn(true, wait = true)
                }
                200 -> errAlert("상대가 게임을 떠났습니다.").show()
                201 -> errAlert("서버와의 연결이 끊어졌습니다.").show()
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
        clearDice()
        setBtn(false, wait = true)

        with (btnScore) {
            setOnClickListener {
                with(scoreboard) {
                    visibility = if (visibility == View.VISIBLE) {
                        text = "점수 보기"
                        View.INVISIBLE
                    } else {
                        text = "점수 닫기"
                        View.VISIBLE
                    }
                }
            }
        }

        btnConfirm.setOnClickListener {
            if (!isMyTurn && !isThrowable) {
                shuffle()
                showAll(first = true)
                setBtn(false, wait = true)
                U.send(70 + dice.sum())
            } else {
                if (isThrowable) {
                    shuffle()
                    calculate()
                    showAll()
                    setBtn(false, --remain == 0)
                } else {
                    setBtn(true)
                    clearDice()
                }
                isThrowable = !isThrowable
            }
        }
    }
}