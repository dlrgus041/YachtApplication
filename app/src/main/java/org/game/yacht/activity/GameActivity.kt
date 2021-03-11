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
    private val notice by lazy { findViewById<TextView>(R.id.notice) }
    private val fixed by lazy { findViewById<LinearLayout>(R.id.fixed) }
    private val roll by lazy { findViewById<LinearLayout>(R.id.roll) }
    private val btnScore by lazy { findViewById<Button>(R.id.btnScore) }
    private val btnConfirm by lazy { findViewById<Button>(R.id.btnConfirm) }
    private val time by lazy { findViewById<CircleProgressBar>(R.id.time) }

    private val level = arrayOf(
            "", "Aces", "Dueces", "Threes", "Fours", "Fives", "Sixes",
            "Choice", "4 of a Kind", "Full House", "S. Straight", "L. Straight", "Yacht"
    )

    private val scoreDialog by lazy { AlertDialog.Builder(this).setTitle("확인")
            .setNegativeButton("아니오") {_, _ -> }.create() }

    private val exitGame by lazy {
        AlertDialog.Builder(this).setTitle("경고").setMessage("자동 패배 처리 됩니다. 게속 하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    U.send(200)
                    U.close()
                }
                .setNegativeButton("아니오") { _, _ -> }.create()
    }

    private val img = Array(7) { 0 }
    private val dice = Array(5) { 0 }
    private val cnt = Array(7) { 0 }

    private var isMyTurn = false
    private var isThrowable = false
    private var remain = 3

    private fun initialize() {
        remain = 3
        for (i in 0 .. 4) {
            fixed[i].visibility = View.GONE
            with (roll[i] as ImageView) {
                setImageResource(img[0])
                visibility = View.VISIBLE
            }
        }
    }

    private fun initScore(opponent: Boolean = false) {
        for (board in 0..1) {
            for (index in 1..6) setScore(board, index, opponent, true)
        }
    }

    private fun hintScore(table: TableLayout, category: Int, value: Int, opponent: Boolean) {
        ((table[if (opponent) (U.player + 1) % 2 else U.player] as TableRow)[category] as TextView).hint = value.toString()
    }

    private fun setScore(board: Int, index: Int, opponent: Boolean = false, init : Boolean = false) =
            with(((scoreboard[board] as TableLayout)[if (opponent) 3 - U.player else U.player] as TableRow)[index] as TextView) {
                text = if (init) "0" else {
                    response(if (opponent) 70 else (100 + 10 * board + index))
                    initialize()
                    isMyTurn = !isMyTurn
                    hint
                }
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
        with(scoreboard[0] as TableLayout) {
            for (i in 1..6) hintScore(this, i, dice.filter { it == i }.sum(), opponent)
        }
        val mask = judge()
        with(scoreboard[1] as TableLayout) {
            hintScore(this, 1, dice.sum(), opponent)
            hintScore(this, 2, if (mask and FOUR_CARD != 0) dice.sum() else 0, opponent)
            hintScore(this, 3, if (mask and FULL_HOUSE != 0) dice.sum() else 0, opponent)
            hintScore(this, 4, if (mask and S_STRAIGHT != 0) 15 else 0, opponent)
            hintScore(this, 5, if (mask and L_STRAIGHT != 0) 30 else 0, opponent)
            hintScore(this, 6, if (mask and YACHT != 0) dice.sum() else 0, opponent)
        }
    }

    private fun clearDice() {
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
            if (roll[i].visibility != View.VISIBLE) continue
            if (!first) U.send(10 * i + dice[i])
            U.handle(U.gHdl, 10 * i + 7)
            Thread.sleep(200)
        }
        if (!first) U.handle(U.gHdl, 60)
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

    private fun setBtn() = with (btnConfirm) {
        notice.text = if (btnScore.isEnabled) "주사위를 굴려서 조합을 만드세요."
        else "주사위 눈의 합이 더 큰 사람이 선공입니다."
        text = "굴린다!"
        isEnabled = true
    }

    private fun response(code: Int = 255) {
        notice.text = "상대를 기다리는 중..."
        btnConfirm.text = "대기중"
        btnConfirm.isEnabled = false
        U.send(code)
    }

    private fun errAlert(message: String) = AlertDialog.Builder(this).setTitle("알림")
            .setMessage(message).setNeutralButton("확인") { _, _ -> }.create()

    override fun onBackPressed() = exitGame.show()

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

        for (board in 0 .. 1) {
            for (index in 1 .. 6) {
                    with (((scoreboard[board] as TableLayout)[U.player] as TableRow)[index] as TextView) {
                        setOnClickListener {
                            if (isMyTurn && !isThrowable) scoreDialog
                                    .apply { setTitle("${level[(board + 1) * index]} ${hint}점을 획득하시겠습니까?") }
                                    .apply { setButton(AlertDialog.BUTTON_POSITIVE, "예") {_, _ -> setScore(board, index) } }
                                    .show()

                        }
                    }
            }
        }

        for (i in 0 .. 6)
            img[i] = resources.getIdentifier("dice$i", "drawable", packageName)

        U.gHdl = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message): Unit = when (msg.what) {
                in 0 .. 49 -> display(msg.what / 10, (msg.what % 10))
                in 50 .. 54 -> setDice(false, msg.what - 50)
                in 55 .. 59 -> setDice(true, msg.what - 55)
                60 -> response()
                61 -> {
                    U.toast(this@GameActivity, "후공입니다.")
                    notice.text = "상대의 차례입니다."
                    clearDice()
                    btnScore.isEnabled = true
                }
                69 -> {
                    U.toast(this@GameActivity, "선공입니다.")
                    isMyTurn = true
                    isThrowable = true
                    clearDice()
                    btnScore.isEnabled = true
                    setBtn()
                }
                70 -> {
                    if (isMyTurn) {
                        setBtn()
                        isThrowable = true
                    } else {
                        notice.text = "상대의 차례입니다."
                        U.send(70)
                    }
                }
                in 101 .. 119 -> {
                    val int = msg.what - 100
                    setScore(int / 10, int % 10, true)
                }
                120 -> {
                    if (isMyTurn) setBtn() else {
                        clearDice()
                        initScore(true)
                        U.send(120)
                    }
                }
//                200 -> errAlert("서버와의 연결이 끊어졌습니다.").show()
                201 -> errAlert("상대가 게임을 떠났습니다.").show()
                255 -> {
                    if (btnScore.isEnabled) {
                        if (isMyTurn) {
                            notice.text = if (--remain > 0) {
                                btnConfirm.also { it.text = "선택 완료" }.isEnabled = true
                                "남은 기회 : ${remain}회"
                            } else "점수표에서 획득할 점수를 선택하세요."
                        } else {
                            calculate(true)
                            U.send(255)
                        }
                    } else setBtn()
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
        clearDice()
        response()

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
                response(70 + dice.sum())
            } else {
                if (isThrowable) {
                    shuffle()
                    calculate()
                    showAll()
                } else {
                    initScore()
                    response(120)
                }
                isThrowable = !isThrowable
            }
        }
    }
}