package org.game.yacht.activity

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.dinuscxj.progressbar.CircleProgressBar
import org.game.yacht.R
import org.game.yacht.util.U

class GameActivity: AppCompatActivity() {

    private val scoreboard by lazy { findViewById<LinearLayout>(R.id.scoreboard) }
    private val notice by lazy { findViewById<TextView>(R.id.notice) }
    private val fixed by lazy { findViewById<LinearLayout>(R.id.fixed) }
    private val roll by lazy { findViewById<LinearLayout>(R.id.roll) }
    private val btnScore by lazy { findViewById<Button>(R.id.btnScore) }
    private val btnConfirm by lazy { findViewById<Button>(R.id.btnConfirm) }
    private val time by lazy { findViewById<CircleProgressBar>(R.id.time) }

    private val p1Name by lazy { findViewById<TextView>(R.id.p1Name) }
    private val p2Name by lazy { findViewById<TextView>(R.id.p2Name) }
    private var player = -1

    private val level = arrayOf(
            "", "Aces", "Deuces", "Threes", "Fours", "Fives", "Sixes",
            "Choice", "4 of a Kind", "Full House", "S. Straight", "L. Straight", "Yacht"
    )
    private val lambda = with (U) { arrayOf<(Int) -> Int>({0},
        { dice.filter { it == 1 }.sum() },
        { dice.filter { it == 2 }.sum() },
        { dice.filter { it == 3 }.sum() },
        { dice.filter { it == 4 }.sum() },
        { dice.filter { it == 5 }.sum() },
        { dice.filter { it == 6 }.sum() },
        { dice.sum() },
        { i -> if (i and FOUR_CARD > 0) dice.sum() else 0 },
        { i -> if (i and FULL_HOUSE > 0) dice.sum() else 0 },
        { i -> if (i and S_STRAIGHT > 0) 15 else 0 },
        { i -> if (i and L_STRAIGHT > 0) 30 else 0 },
        { i -> if (i and YACHT > 0) dice.sum() else 0 })
    }

    private val scoreDialog by lazy { AlertDialog.Builder(this).setTitle("??????")
            .setNegativeButton("?????????") {_, _ -> }.create() }

    private val rematchDialog by lazy { AlertDialog.Builder(this).setTitle("??????").setMessage("???????????? ???????????? ??????????????????????")
            .setPositiveButton("???") { _, _ -> response(62)}
            .setNegativeButton("?????????") { _, _ ->
                U.send(68)
                U.close()
            }
    }

    private val gameResult by lazy { AlertDialog.Builder(this).setNeutralButton("??????") { _, _ -> rematchDialog.show()}.create() }

    private val exitGame by lazy {
        AlertDialog.Builder(this).setTitle("??????").setMessage("?????? ?????? ?????? ?????????. ?????? ???????????????????")
                .setPositiveButton("???") { _, _ ->
                    U.send(200)
                    U.close()
                }.setNegativeButton("?????????") { _, _ -> }.create()
    }

    private val img = Array(7) { 0 }
    private val dice = Array(5) { 0 }
    private val cnt = Array(7) { 0 }

    private var isMyTurn = false
    private var isThrowable = false
    private var remain = 3
    private val total = Array(3) { 0 }
    private val bonus = Array(3) { 0 }

    private fun getScorePos(board: Int, index: Int, opponent: Boolean) =
            ((scoreboard[board] as TableLayout)[if (opponent) 3 - player else player] as TableRow)[index] as TextView

    private fun initialize(code: Int = 255, rematch: Boolean = false) {
        remain = 3
        for (i in 0 .. 4) {
            dice[i] = 0
            fixed[i].visibility = View.GONE
            with (roll[i] as ImageView) {
                setImageResource(img[0])
                visibility = View.VISIBLE
            }
        }
        if (rematch) {
            isMyTurn = false
            isThrowable = false
            for (i in 0..4) dice[i] = 0
            for (i in 1..2) {
                total[i] = 0
                bonus[i] = 0
            }
        }
        response(code)
    }

    private fun hintScore(board: Int, index: Int, value: Any, opponent: Boolean) {
        getScorePos(board, index, opponent).hint = value.toString()
    }

    private fun setScore(board: Int, index: Int, opponent: Boolean = false) =
            with(getScorePos(board, index, opponent)) {
                text = hint.also {
                    val p = if (opponent) {
                        val pos = 6 * (board - 1) + index
                        U.toast(context, "????????? ${level[pos]} ${it}?????? ??????????????????.")
                        3 - player
                    } else player

                    if (board == 1) {
                        bonus[p] += it.toString().toInt()
                        if (bonus[p] >= 65) {
                            total[p] += 35
                            bonus[p] = 0
                            getScorePos(1, 8, opponent).text = "+35"
                            U.toast(context, "?????? 35?????? ?????????????????????!!")
                        }

                    total[p] += it.toString().toInt()
                    getScorePos(2, 8, opponent).text = total[p].toString()
                    }
                }
                initialize(code = if (opponent) 70 else (100 + 10 * board + index))
                this.isEnabled = false
                isMyTurn = !isMyTurn
            }

    private fun mask() = with (U) {
        var ret = 0
        for (i in 0 .. 6) cnt[i] = 0
        for (i in 0 .. 4) ++cnt[dice[i]]

        if (cnt[2] * cnt[3] * cnt[4] * cnt[5] == 1 && (cnt[1] == 1 || cnt[6] == 1))
            ret = ret or L_STRAIGHT
        if (cnt[3] * cnt[4] > 0 && (cnt[1] * cnt[2] > 0 || cnt[2] * cnt[5] > 0 || cnt[5] * cnt[6] > 0))
            ret = ret or S_STRAIGHT

        cnt.sortDescending()
        if (cnt[0] == 5) ret = ret or YACHT or FULL_HOUSE
        if (cnt[0] >= 4) ret = ret or FOUR_CARD
        if (cnt[0] == 3 && cnt[1] == 2) ret = ret or FULL_HOUSE

        ret
    }

    private fun calculate(opponent: Boolean = false, clear: Boolean = false) {
        val m = mask()
        for (t in 1 .. 2) {
            for (i in 1 .. 6) hintScore(t, i, if (clear) "" else lambda[6 * (t - 1) + i](m), opponent)
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
        notice.text = if (btnScore.isEnabled) "???????????? ????????? ????????? ????????????."
        else "????????? ?????? ?????? ??? ??? ????????? ???????????????."
        text = "?????????!"
        isEnabled = true
    }

    private fun response(code: Int = 255) {
        notice.text = "????????? ???????????? ???..."
        btnConfirm.text = "?????????"
        btnConfirm.isEnabled = false
        U.send(code)
    }

    private fun errAlert(message: String) = AlertDialog.Builder(this).setTitle("??????")
            .setMessage(message).setNeutralButton("??????") { _, _ -> }.create()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onBackPressed() = exitGame.show()

    override fun onCreate(savedInstanceState: Bundle?) = with (U) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(R.layout.activity_game)

        gHdl = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message): Unit = when (msg.what) {
                in 0 .. 49 -> display(msg.what / 10, (msg.what % 10))
                in 50 .. 54 -> setDice(false, msg.what - 50)
                in 55 .. 59 -> setDice(true, msg.what - 55)
                60 -> {
                    response()
                    calculate()
                }
                61 -> {
                    toast(this@GameActivity, "???????????????.")
                    notice.text = "????????? ???????????????."
                    clearDice()
                    btnScore.isEnabled = true
                }
                63 -> {
                    toast(this@GameActivity, "????????? ????????? ????????? ??????????????????.")
                    initialize(rematch = true)
                }
                67 -> {
                    toast(this@GameActivity, "???????????? ?????????????????????.")
                    close()
                }
                69 -> {
                    toast(this@GameActivity, "???????????????.")
                    isMyTurn = true
                    isThrowable = true
                    clearDice()
                    btnScore.isEnabled = true
                    setBtn()
                }
                70 -> {
                    if (isMyTurn) {
                        setBtn()
                        calculate(opponent = true, clear = true)
                        isThrowable = true
                    } else {
                        notice.text = "????????? ???????????????."
                        send(70)
                    }
                }
                in 111 .. 129 -> {
                    val int = msg.what - 100
                    setScore(int / 10, int % 10, true)
                }
                130 -> {
                    clearDice()
                    calculate(opponent = !isMyTurn, clear = true)
                    if (isMyTurn) setBtn() else send(130)
                }
//                200 -> errAlert("???????????? ????????? ??????????????????.").show()
                201 -> errAlert("????????? ????????? ???????????????.").show()
                254 -> gameResult.apply {
                    btnScore.isEnabled = false
                    val opponent = 3 - player

                    if (total[player] > total[opponent]) {
                        setTitle("???????????????!")
                        setMessage("?????? ???????????????.")
                    } else if (total[player] < total[opponent]) {
                        setTitle("????????????!")
                        setMessage("????????? ???????????????.")
                    } else {
                        setTitle("??????")
                        setMessage("????????? ???????????????.")
                    }
                }.show()
                255 -> {
                    if (btnScore.isEnabled) {
                        if (isMyTurn) {
                            isThrowable = false
                            notice.text = if (--remain > 0) {
                                btnConfirm.apply { text = "?????? ??????" }.isEnabled = true
                                "?????? ?????? : ${remain}???"
                            } else "??????????????? ????????? ????????? ???????????????."
                        } else {
                            calculate(true)
                            send(255)
                        }
                    } else setBtn()
                }
                else -> toast(this@GameActivity, "Undefined Code (${msg.what})")
            }
        }

        input.handler = gHdl
        time.max = 60
        initialize()

        with(intent) {
            player = getIntExtra("player", -1)
            var p1 = getStringExtra("myName")
            var p2 = getStringExtra("opName")
            if (player == 2) p1 = p2.also { p2 = p1 }
            p1Name.text = p1
            p2Name.text = p2
        }

        for (board in 1 .. 2) {
            for (index in 1 .. 6) {
                with(getScorePos(board, index, opponent = false)) {
                    setOnClickListener {
                        if (isMyTurn && !isThrowable) scoreDialog.apply {
                            setTitle("${level[6 * (board - 1) + index]} ${hint}?????? ?????????????????????????")
                            setButton(AlertDialog.BUTTON_POSITIVE, "???") { _, _ -> setScore(board, index) }
                        }.show()
                    }
                }
            }
        }

        for (i in 0 .. 6)
            img[i] = resources.getIdentifier("dice$i", "drawable", packageName)

        for (i in 0 .. 4) {
            fixed[i].visibility = View.GONE
            roll[i].setOnClickListener {
                if (isMyTurn && !isThrowable) {
                    send(55 + i)
                    setDice(true, i)
                }
            }
            fixed[i].setOnClickListener {
                if (isMyTurn && !isThrowable) {
                    send(50 + i)
                    setDice(false, i)
                }
            }
        }

        with (btnScore) {
            setOnClickListener {
                with(scoreboard) {
                    visibility = if (visibility == View.VISIBLE) {
                        text = "?????? ??????"
                        View.INVISIBLE
                    } else {
                        text = "?????? ??????"
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
                    showAll()
                } else {
                    response(130)
                    isThrowable = true
                }
            }
        }
    }
}