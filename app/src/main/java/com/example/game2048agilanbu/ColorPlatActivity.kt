package com.example.game2048agilanbu;

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.FrameLayout
import android.widget.SeekBar
import android.os.Bundle
import android.view.View
import android.widget.Toast

class ColorPlatActivity : AppCompatActivity(), OnSeekBarChangeListener {
    // UIs
    private var mGameViewFrameLayout: FrameLayout? = null
    private var mSeekBarRed: SeekBar? = null
    private var mSeekBarGreen: SeekBar? = null
    private var mSeekBarBlue: SeekBar? = null

    // Game view
    private var mGameView: MainView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)
        mGameViewFrameLayout = findViewById(R.id.game_view_frame_layout)
        mSeekBarRed = findViewById(R.id.seekbar_red)
        mSeekBarGreen = findViewById(R.id.seekbar_green)
        mSeekBarBlue = findViewById(R.id.seekbar_blue)
        mGameView = MainView(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        mGameView!!.layoutParams = params
        mGameViewFrameLayout!!.addView(mGameView)
        mGameView!!.setBackgroundColor(
            Color.argb(
                255, mSeekBarRed!!.getProgress(),
                mSeekBarGreen!!.getProgress(), mSeekBarBlue!!.getProgress()
            )
        )
        mSeekBarRed!!.setOnSeekBarChangeListener(this)
        mSeekBarGreen!!.setOnSeekBarChangeListener(this)
        mSeekBarBlue!!.setOnSeekBarChangeListener(this)
    }

    fun AcceptColor(view: View?) {
        PrimaryMenuActivity.mBackgroundColor = Color.argb(
            255, mSeekBarRed!!.progress,
            mSeekBarGreen!!.progress, mSeekBarBlue!!.progress
        )
        Toast.makeText(this, getString(R.string.background_color_changed), Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    fun ResetToDefaultColor(view: View?) {
        PrimaryMenuActivity.mBackgroundColor = -0x50711
        Toast.makeText(
            this,
            getString(R.string.background_color_has_been_reset_to_default),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    fun FinishActivity(view: View?) {
        finish()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        mGameView!!.setBackgroundColor(
            Color.argb(
                255, mSeekBarRed!!.progress,
                mSeekBarGreen!!.progress, mSeekBarBlue!!.progress
            )
        )
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}