package com.example.game2048agilanbu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import android.preference.PreferenceManager
import android.view.KeyEvent

class PrimaryActivity : AppCompatActivity() {
    private var view: MainView? = null

    // tag for debug logging
    val TAG = "TanC"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        val frameLayout = findViewById<FrameLayout>(R.id.game_frame_layout)
        view = MainView(this, this)
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        view!!.hasSaveState = settings.getBoolean("save_state", false)
        if (savedInstanceState != null) if (savedInstanceState.getBoolean("hasState")) load()
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        view!!.layoutParams = params
        frameLayout.addView(view)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) return true else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view!!.game.move(2)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view!!.game.move(0)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view!!.game.move(3)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view!!.game.move(1)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("hasState", true)
        save()
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private val isNewFeaturesDialogShowed: Boolean
        private get() {
            val settings = PreferenceManager.getDefaultSharedPreferences(this)
            return settings.getBoolean("has_new_dialog_showed_1", false)
        }

    private fun turnOffNewFeaturesDialogShowed() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        editor.putBoolean("has_new_dialog_showed_1", true)
        editor.apply()
    }

    private fun save() {
        val rows: Int = PrimaryMenuActivity.rows
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        val field = view!!.game.grid!!.field
        val undoField = view!!.game.grid!!.undoField
        editor.putInt(WIDTH + rows, field.size)
        editor.putInt(HEIGHT + rows, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                if (field[xx][yy] != null) editor.putInt(
                    "$rows $xx $yy",
                    field[xx][yy]!!.value
                ) else editor.putInt(
                    "$rows $xx $yy", 0
                )
                if (undoField[xx][yy] != null) editor.putInt(
                    UNDO_GRID + rows + " " + xx + " " + yy,
                    undoField[xx][yy]!!.value
                ) else editor.putInt(
                    UNDO_GRID + rows + " " + xx + " " + yy, 0
                )
            }
        }

        // reward deletions:
        editor.putInt(REWARD_DELETES + rows, mRewardDeletes)
        editor.putInt(REWARD_DELETE_SELECTION + rows, mRewardDeletingSelectionAmounts)

        // game values:
        editor.putLong(SCORE + rows, view!!.game.score)
        editor.putLong(HIGH_SCORE + rows, view!!.game.highScore)
        editor.putLong(UNDO_SCORE + rows, view!!.game.lastScore)
        editor.putBoolean(CAN_UNDO + rows, view!!.game.canUndo)
        editor.putInt(GAME_STATE + rows, view!!.game.gameState)
        editor.putInt(UNDO_GAME_STATE + rows, view!!.game.lastGameState)
        editor.apply()
        when (PrimaryMenuActivity.rows) {
            4 -> mHighScore4x4 = view!!.game.highScore
            5 -> mHighScore5x5 = view!!.game.highScore
            6 -> mHighScore6x6 = view!!.game.highScore
        }
    }

    private fun load() {
        val rows: Int = PrimaryMenuActivity.rows

        //Stopping all animations
        view!!.game.aGrid!!.cancelAnimations()
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        for (xx in view!!.game.grid!!.field.indices) {
            for (yy in 0 until view!!.game.grid!!.field[0].size) {
                val value = settings.getInt("$rows $xx $yy", -1)
                if (value > 0) view!!.game.grid!!.field[xx][yy] =
                    GridTiles(xx, yy, value) else if (value == 0) view!!.game.grid!!.field[xx][yy] = null
                val undoValue = settings.getInt(UNDO_GRID + rows + " " + xx + " " + yy, -1)
                if (undoValue > 0) view!!.game.grid!!.undoField[xx][yy] = GridTiles(
                    xx,
                    yy,
                    undoValue
                ) else if (value == 0) view!!.game.grid!!.undoField[xx][yy] = null
            }
        }
        mRewardDeletes = settings.getInt(REWARD_DELETES + rows, 2)
        mRewardDeletingSelectionAmounts = settings.getInt(REWARD_DELETE_SELECTION + rows, 3)
        view!!.game.score = settings.getLong(SCORE + rows, view!!.game.score)
        view!!.game.highScore = settings.getLong(HIGH_SCORE + rows, view!!.game.highScore)
        view!!.game.lastScore = settings.getLong(UNDO_SCORE + rows, view!!.game.lastScore)
        view!!.game.canUndo = settings.getBoolean(CAN_UNDO + rows, view!!.game.canUndo)
        view!!.game.gameState = settings.getInt(GAME_STATE + rows, view!!.game.gameState)
        view!!.game.lastGameState =
            settings.getInt(UNDO_GAME_STATE + rows, view!!.game.lastGameState)
    }

    private val isEmptyAchievementsOrLeaderboards: Boolean
        private get() = (!mAchievement32 || !mAchievement64 || !mAchievement128 || !mAchievement256
                || !mAchievement512 || !mAchievement1024 || !mAchievement2048 || !mAchievement4096
                || !mAchievement8192 || mHighScore4x4 < 0 || mHighScore5x5 < 0 || mHighScore6x6 < 0)

    companion object {
        @JvmField
        var mRewardDeletes = 2

        // delete selection:
        @JvmField
        var mRewardDeletingSelectionAmounts = 3
        private const val REWARD_DELETES = "reward chances"
        private const val WIDTH = "width"
        private const val HEIGHT = "height"
        private const val SCORE = "score"
        private const val HIGH_SCORE = "high score temp"
        private const val UNDO_SCORE = "undo score"
        private const val CAN_UNDO = "can undo"
        private const val UNDO_GRID = "undo"
        private const val GAME_STATE = "game state"
        private const val UNDO_GAME_STATE = "undo game state"
        private const val REWARD_DELETE_SELECTION = "reward delete selection amounts"

        // request codes we use when invoking an external activity
        const val RC_SIGN_IN = 9001

        // achievements and scores we're pending to push to the cloud
        // (waiting for the user to sign in, for instance)
        private var mHighScore4x4: Long = 0
        private var mHighScore5x5: Long = 0
        private var mHighScore6x6: Long = 0
        private var mAchievement32 = false
        private var mAchievement64 = false
        private var mAchievement128 = false
        private var mAchievement256 = false
        private var mAchievement512 = false
        private var mAchievement1024 = false
        private var mAchievement2048 = false
        private var mAchievement4096 = false
        private var mAchievement8192 = false
        fun unlockAchievement(requestedTile: Int) {
            // Check if each condition is met; if so, unlock the corresponding achievement.
            if (requestedTile == 32) mAchievement32 = true
            if (requestedTile == 64) mAchievement64 = true
            if (requestedTile == 128) mAchievement128 = true
            if (requestedTile == 256) mAchievement256 = true
            if (requestedTile == 512) mAchievement512 = true
            if (requestedTile == 1024) mAchievement1024 = true
            if (requestedTile == 2048) mAchievement2048 = true
            if (requestedTile == 4096) mAchievement4096 = true
            if (requestedTile == 8192) mAchievement8192 = true
        }
    }
}