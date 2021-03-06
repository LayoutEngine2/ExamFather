package xfy.wl.examfather

import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_game.*
import org.jetbrains.anko.alert
import xfy.wl.examfather.storage.AppPreferences


class GameActivity : AppCompatActivity(), GameFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    var is_odd: Boolean = true
    private var countDownTimer: ExamTimer? = null
    private var currentTimeMS = 45 * 60_000L
    var finishAnswer = 0
    var rightAnswer = 0

    private var listener: IntentSender.OnFinished? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        preferences = AppPreferences(this)

        GameActivity.rightTextView = findViewById<TextView>(R.id.main_right_tx)
        GameActivity.errorTextView = findViewById<TextView>(R.id.main_error_tx)
        GameActivity.totalTextView = findViewById(R.id.main_total_tx)
        GameActivity.game_pager = findViewById(R.id.main_viewpager)
        main_total_tx.text = "1/" + ExamDBHelper.examInfoList.size.toString()

        main_bar.setOnClickListener(this::onClickOpenBottomSheetDialog)
        question_back.setOnClickListener(this::onClickBackHandler)
        main_viewpager.adapter = GameViewPagerAdapter(this)
        main_viewpager.registerOnPageChangeCallback(viewPagerOnChangeCallback)

        isExam = intent.getBooleanExtra(IS_EXAM, true)

        if (!isExam) {//如果不是考试,就隐藏
            question_timer.visibility = View.GONE
            question_submit.visibility = View.GONE
            val preferences = AppPreferences(this)
            main_viewpager.currentItem = preferences.getCurrentExamPage()
        } else {
            startTimer()
            question_submit.setOnClickListener(this::onClickBackHandler)
        }
    }

    private fun startTimer() {
        countDownTimer = object : ExamTimer(currentTimeMS, 1000) {
            //2700 45分钟
            override fun onTick(l: Long) {
                currentTimeMS = l
                val allSecond = l.toInt() / 1000//秒
                val minute = allSecond / 60
                val second = allSecond - minute * 60
                question_countdown.text = "倒计时 $minute:$second"
            }

            override fun onFinish() {
                submitAnswer()
            }
        }
        countDownTimer?.start()
    }

    var bottomSheetDialog: BottomSheetDialog? = null

    private fun onClickOpenBottomSheetDialog(view: View) {
        bottomSheetDialog = BottomSheetDialog(this)
        val gridView = GridView(this)
        gridView.numColumns = 6
        gridView.verticalSpacing = 10
        gridView.horizontalSpacing = 10
        gridView.setBackgroundColor(-0x1)
        gridView.adapter = AnswerAdapter()
        gridView.scrollBarStyle = GridView.SCROLLBARS_OUTSIDE_INSET
        gridView.setPadding(20, 20, 20, 20)
        bottomSheetDialog?.setContentView(gridView)
        bottomSheetDialog?.show()
        gridView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                main_viewpager.setCurrentItem(position, false)
                bottomSheetDialog?.dismiss()
            }
    }

    //提交答案
    fun submitAnswer() {
        val preferences = AppPreferences(this)
        preferences.saveTopScore(rightAnswer)
        MainActivity.setTopScore(preferences.getTopScore())
        finish()
    }

    private fun onClickBackHandler(view: View) {
        if (!isExam) {
            this@GameActivity.finish()
            return
        }

        finishAnswer = 0
        rightAnswer = 0
        var isBackButton = view.id == R.id.question_back
        var buttonText = if (isBackButton) "直接返回" else "取消交卷"

        for (exam in ExamDBHelper.examInfoList) {
            if (exam.isRightChoice)
                rightAnswer++
            if (exam.hasFinishedResult)
                finishAnswer++
        }

        val message = "您已回答了" + finishAnswer + "题(共100题),考试得分" + rightAnswer + ",确定交卷？"
        alert(message, "温馨提示") {
            positiveButton("确定交卷") {
                submitAnswer()
            }
            negativeButton(buttonText) {
                if (view.id == question_back.id) {
                    this@GameActivity.finish()
                }
            }
        }.show()
    }

    companion object {
        var rightTextView: TextView? = null
        var errorTextView: TextView? = null
        var totalTextView: TextView? = null
        var game_pager: ViewPager2? = null
        val IS_EXAM = "isExam"
        var isExam: Boolean = false
        lateinit var preferences: AppPreferences

        fun nextQuestion() {
            game_pager!!.currentItem++
        }


        fun updateRightAndError() {
            var rightCount = 0
            var errorCount = 0
            for (examInfo in ExamDBHelper.examInfoList) {
                if (examInfo.hasFinishedResult) {
                    if (examInfo.isRightChoice) {
                        rightCount++
                    } else {
                        errorCount++
                    }
                }
            }
            rightTextView?.text = rightCount.toString()
            errorTextView?.text = errorCount.toString()
        }
    }



    private inner class GameViewPagerAdapter(ga: GameActivity) : FragmentStateAdapter(ga){
        override fun createFragment(position: Int): Fragment {
            val gameFragment = GameFragment()
            val bundle = Bundle()
            bundle.putInt("position", position)
            bundle.putBoolean("isExam", GameActivity.isExam)
            gameFragment.arguments = bundle

            return gameFragment
        }

        override fun getItemCount(): Int = ExamDBHelper.examInfoList.size

    }

    private val viewPagerOnChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (!isExam) {
                preferences.saveCurrentExamPage(position)
            }
            GameActivity.totalTextView!!.text = (1 + position).toString() + "/" + ExamDBHelper.examInfoList.size.toString()
        }
    }
}




