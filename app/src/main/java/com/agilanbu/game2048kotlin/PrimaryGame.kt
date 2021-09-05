package com.agilanbu.game2048kotlin

import android.app.AlertDialog
import android.content.Context
import android.preference.PreferenceManager
import android.widget.Toast
import java.util.*

class PrimaryGame(private val mContext: Context, private val mView: PrimaryView) {
    @JvmField
    var mGameState = GAME_NORMAL
    @JvmField
    var mLastGameState = GAME_NORMAL
    private var mBufferGameState = GAME_NORMAL
    @JvmField
    var mGrid: GridItems? = null
    @JvmField
    var aGrid: AnimGridItems? = null
    @JvmField
    var mCanUndo = false
    @JvmField
    var mScore: Long = 0
    @JvmField
    var mHighScore: Long = 0
    @JvmField
    var mLastScore: Long = 0
    private var bufferScore: Long = 0
    fun newGame() {
        val rows: Int = PrimaryMenuActivity.rows
        if (mGrid == null) mGrid = GridItems(rows, rows) else {
            prepareUndoState()
            saveUndoState()
            mGrid!!.clearGrid()
        }
        aGrid = AnimGridItems(rows, rows)
        mHighScore = getHighScore()
        if (mScore >= mHighScore) {
            mHighScore = mScore
            recordHighScore()
        }
        mScore = 0
        mGameState = GAME_NORMAL
        addStartTiles()
        mView.mRefreshLastTime = true
        mView.resyncTime()
        mView.invalidate()
    }

    private fun addStartTiles() {
        val startTiles = 2
        for (xx in 0 until startTiles) addRandomTile()
    }

    private fun addRandomTile() {
        if (mGrid!!.isCellsAvailable) {
            val value = if (Math.random() < 0.9) 2 else 4
            val tile = GridTiles(mGrid!!.randomAvailableCell()!!, value)
            spawnTile(tile)
        }
    }

    private fun spawnTile(tile: GridTiles) {
        mGrid!!.insertTile(tile)
        aGrid!!.startAnimation(
            tile.x, tile.y, SPAWN_ANIMATION,
            SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null
        ) //Direction: -1 = EXPANDING
    }

    private fun recordHighScore() {
        val rows: Int = PrimaryMenuActivity.rows
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        editor.putLong(HIGH_SCORE + rows, mHighScore)
        editor.apply()
    }

    private fun getHighScore(): Long {
        val rows: Int = PrimaryMenuActivity.rows
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        return settings.getLong(HIGH_SCORE + rows, -1)
    }

    private fun prepareTiles() {
        for (array in mGrid!!.mField) for (tile in array) if (mGrid!!.isCellOccupied(tile)) tile!!.mMergedFrom =
            null
    }

    private fun moveTile(tile: GridTiles, cell: CellItem) {
        mGrid!!.mField[tile.x][tile.y] = null
        mGrid!!.mField[cell.x][cell.y] = tile
        tile.updatePosition(cell)
    }

    private fun saveUndoState() {
        mGrid!!.saveTiles()
        mCanUndo = true
        mLastScore = bufferScore
        mLastGameState = mBufferGameState
    }

    private fun prepareUndoState() {
        mGrid!!.prepareSaveTiles()
        bufferScore = mScore
        mBufferGameState = mGameState
    }

    fun revertUndoState() {
        if (mCanUndo) {
            mCanUndo = false
            aGrid!!.cancelAnimations()
            mGrid!!.revertTiles()
            mScore = mLastScore
            mGameState = mLastGameState
            mView.mRefreshLastTime = true
            mView.invalidate()
        }
    }

    fun gameWon(): Boolean {
        return mGameState > 0 && mGameState % 2 != 0
    }

    fun gameLost(): Boolean {
        return mGameState == GAME_LOST
    }

    val isActive: Boolean
        get() = !(gameWon() || gameLost())

    fun move(direction: Int) {
        aGrid!!.cancelAnimations()
        // 0: up, 1: right, 2: down, 3: left
        if (!isActive) return
        prepareUndoState()
        val vector = getVector(direction)
        val traversalsX = buildTraversalsX(vector)
        val traversalsY = buildTraversalsY(vector)
        var moved = false
        prepareTiles()
        for (xx in traversalsX) {
            for (yy in traversalsY) {
                val cell = CellItem(xx, yy)
                val tile = mGrid!!.getCellContent(cell)
                if (tile != null) {
                    val positions = findFarthestPosition(cell, vector)
                    val next = mGrid!!.getCellContent(positions[1])
                    if (next != null && next.mValue == tile.mValue && next.mMergedFrom == null) {
                        val merged = GridTiles(positions[1], tile.mValue * 2)
                        val temp = arrayOf(tile, next)
                        merged.mMergedFrom = temp
                        mGrid!!.insertTile(merged)
                        mGrid!!.removeTile(tile)

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1])
                        val extras = intArrayOf(xx, yy)
                        aGrid!!.startAnimation(
                            merged.x, merged.y, MOVE_ANIMATION,
                            MOVE_ANIMATION_TIME, 0, extras
                        ) //Direction: 0 = MOVING MERGED
                        aGrid!!.startAnimation(
                            merged.x, merged.y, MERGE_ANIMATION,
                            SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null
                        )

                        // Update the score
                        mScore = mScore + merged.mValue
                        mHighScore = Math.max(mScore, mHighScore)

                        // The mighty 2048 tile
                        if (merged.mValue >= winValue() && !gameWon()) {
                            mGameState = mGameState + GAME_WIN // Set win state
                            endGame()
                        }
                        if (!PrimaryMenuActivity.mIsMainMenu) {
                            if (merged.mValue >= 32) {
                                PrimaryActivity.unlockAchievement(merged.mValue)
                            }
                        }
                    } else {
                        moveTile(tile, positions[0])
                        val extras = intArrayOf(xx, yy, 0)
                        aGrid!!.startAnimation(
                            positions[0].x,
                            positions[0].y,
                            MOVE_ANIMATION,
                            MOVE_ANIMATION_TIME,
                            0,
                            extras
                        ) //Direction: 1 = MOVING NO MERGE
                    }
                    if (!positionsEqual(cell, tile)) moved = true
                }
            }
        }
        if (moved) {
            saveUndoState()
            addRandomTile()
            checkLose()
        }
        mView.resyncTime()
        mView.invalidate()
    }

    private fun checkLose() {
        if (!movesAvailable() && !gameWon()) {
            mGameState = GAME_LOST
            endGame()
        }
    }

    private fun endGame() {
        aGrid!!.startAnimation(
            -1,
            -1,
            FADE_GLOBAL_ANIMATION,
            NOTIFICATION_ANIMATION_TIME,
            NOTIFICATION_DELAY_TIME,
            null
        )
        if (mScore >= mHighScore) {
            mHighScore = mScore
            recordHighScore()
        }
    }

    private fun getVector(direction: Int): CellItem {
        val map = arrayOf(
            CellItem(0, -1),  // up
            CellItem(1, 0),  // right
            CellItem(0, 1),  // down
            CellItem(-1, 0) // left
        )
        return map[direction]
    }

    private fun buildTraversalsX(vector: CellItem): List<Int> {
        val traversals: MutableList<Int> = ArrayList()
        val rows: Int = PrimaryMenuActivity.rows
        for (xx in 0 until rows) traversals.add(xx)
        if (vector.x == 1) Collections.reverse(traversals)
        return traversals
    }

    private fun buildTraversalsY(vector: CellItem): List<Int> {
        val traversals: MutableList<Int> = ArrayList()
        val rows: Int = PrimaryMenuActivity.rows
        for (xx in 0 until rows) traversals.add(xx)
        if (vector.y == 1) Collections.reverse(traversals)
        return traversals
    }

    private fun findFarthestPosition(cell: CellItem, vector: CellItem): Array<CellItem> {
        var previous: CellItem
        var nextCell = CellItem(cell.x, cell.y)
        do {
            previous = nextCell
            nextCell = CellItem(
                previous.x + vector.x,
                previous.y + vector.y
            )
        } while (mGrid!!.isCellWithinBounds(nextCell) && mGrid!!.isCellAvailable(nextCell))
        return arrayOf(previous, nextCell)
    }

    private fun movesAvailable(): Boolean {
        return mGrid!!.isCellsAvailable || tileMatchesAvailable()
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: GridTiles?
        val rows: Int = PrimaryMenuActivity.rows
        for (xx in 0 until rows) {
            for (yy in 0 until rows) {
                tile = mGrid!!.getCellContent(CellItem(xx, yy))
                if (tile != null) {
                    for (direction in 0..3) {
                        val vector = getVector(direction)
                        val cell = CellItem(xx + vector.x, yy + vector.y)
                        val other = mGrid!!.getCellContent(cell)
                        if (other != null && other.mValue == tile.mValue) return true
                    }
                }
            }
        }
        return false
    }

    private fun positionsEqual(first: CellItem, second: CellItem): Boolean {
        return first.x == second.x && first.y == second.y
    }

    private fun winValue(): Int {
        return if (!canContinue()) endingMaxValue else startingMaxValue
    }

    fun setEndlessMode() {
        mGameState = GAME_ENDLESS
        mView.invalidate()
        mView.mRefreshLastTime = true
    }

    fun canContinue(): Boolean {
        return !(mGameState == GAME_ENDLESS || mGameState == GAME_ENDLESS_WON)
    }

    private fun customSaveLoadTemp() {
        val rows: Int = PrimaryMenuActivity.rows
        val WIDTH = "width" + rows + "temp"
        val HEIGHT = "height" + rows + "temp"
        var deleteAmount = rows - 1

        // Save() as "temp"
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        val field = mGrid!!.mField
        editor.putInt(WIDTH, field.size)
        editor.putInt(HEIGHT, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                if (field[xx][yy] != null) {
                    if (field[xx][yy]!!.mValue >= 2 && field[xx][yy]!!.mValue <= 32 && deleteAmount > 0) {
                        deleteAmount--
                        editor.putInt(rows.toString() + " " + xx + " " + yy + "temp", 0)
                    } else editor.putInt(
                        rows.toString() + " " + xx + " " + yy + "temp",
                        field[xx][yy]!!.mValue
                    )
                } else editor.putInt(rows.toString() + " " + xx + " " + yy + "temp", 0)
            }
        }
        editor.apply()

        // Load() as "temp"
        for (xx in mGrid!!.mField.indices) {
            for (yy in 0 until mGrid!!.mField[0].size) {
                val value = settings.getInt(rows.toString() + " " + xx + " " + yy + "temp", -1)
                if (value > 0) mGrid!!.mField[xx][yy] =
                    GridTiles(xx, yy, value) else if (value == 0) mGrid!!.mField[xx][yy] = null
            }
        }
        mCanUndo = false
        mGameState = mLastGameState
    }

    fun makeToast(resId: Int) {
        Toast.makeText(mContext, mContext.getString(resId), Toast.LENGTH_SHORT).show()
    }

    fun RemoveTilesWithTrash() {
        val rows: Int = PrimaryMenuActivity.rows
        val cellCount = rows * rows - (rows + 2)
        if (mContext.javaClass == ColorPlatActivity::class.java) // because of color picker
        {
            if (mView.mGame.mGrid!!.getAvailableCells().size < cellCount) {
                customSaveLoadTemp()
                mView.invalidate()
            } else mView.mGame.makeToast(R.string.tiles_are_not_enough_to_remove)
        } else {
            if (PrimaryActivity.mRewardDeletes > 0) {
                if (mView.mGame.mGrid!!.getAvailableCells().size < cellCount) {
                    AlertDialog.Builder(mView.context)
                        .setPositiveButton(R.string.yes_delete_tiles) { dialog, which ->
                            PrimaryActivity.mRewardDeletes-- // decrease rewards
                            customSaveLoadTemp()
                            mView.invalidate()
                        }
                        .setNegativeButton(R.string.dont_delete_tiles, null)
                        .setTitle(R.string.trash_dialog_title)
                        .setMessage(R.string.trash_dialog_message)
                        .show()
                } else mView.mGame.makeToast(R.string.tiles_are_not_enough_to_remove)
            } else mView.mGame.makeToast(R.string.reward_amount_error)
        }
    }

    fun loadCurrentBoard() {
        //Stopping all animations
        mView.mGame.aGrid!!.cancelAnimations()
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val rows: Int = PrimaryMenuActivity.rows
        if (!settings.getBoolean("has_snapshot$rows", false)) {
            // if we haven't any snapshot already, so we better don't loading anything
            mView.mGame.makeToast(R.string.loading_failed)
            return
        }
        val CURRENT_STATE = "cs"
        val UNDO_GRID = "undo$rows$CURRENT_STATE"
        val REWARD_DELETES = "reward chances$rows$CURRENT_STATE"
        val SCORE = "score$rows$CURRENT_STATE"
        val UNDO_SCORE = "undo score$rows$CURRENT_STATE"
        val CAN_UNDO = "can undo$rows$CURRENT_STATE"
        val GAME_STATE = "game state$rows$CURRENT_STATE"
        val UNDO_GAME_STATE = "undo game state$rows$CURRENT_STATE"
        for (xx in mView.mGame.mGrid!!.mField.indices) {
            for (yy in 0 until mView.mGame.mGrid!!.mField[0].size) {
                val value = settings.getInt("$CURRENT_STATE$rows $xx $yy", -1)
                if (value > 0) mView.mGame.mGrid!!.mField[xx][yy] =
                    GridTiles(xx, yy, value) else if (value == 0) mView.mGame.mGrid!!.mField[xx][yy] = null
                val undoValue = settings.getInt("$UNDO_GRID$rows $xx $yy", -1)
                if (undoValue > 0) mView.mGame.mGrid!!.mUndoField[xx][yy] = GridTiles(
                    xx,
                    yy,
                    undoValue
                ) else if (value == 0) mView.mGame.mGrid!!.mUndoField[xx][yy] = null
            }
        }
        PrimaryActivity.mRewardDeletes = settings.getInt(REWARD_DELETES, 2)
        mView.mGame.mScore = settings.getLong(SCORE, mView.mGame.mScore)
        mView.mGame.mHighScore = settings.getLong(HIGH_SCORE, mView.mGame.mHighScore)
        mView.mGame.mLastScore = settings.getLong(UNDO_SCORE, mView.mGame.mLastScore)
        mView.mGame.mCanUndo = settings.getBoolean(CAN_UNDO, mView.mGame.mCanUndo)
        mView.mGame.mGameState = settings.getInt(GAME_STATE, mView.mGame.mGameState)
        mView.mGame.mLastGameState = settings.getInt(UNDO_GAME_STATE, mView.mGame.mLastGameState)
        mView.invalidate()
        mView.mGame.makeToast(R.string.loaded)
    }

    fun saveCurrentBoard() {
        if (!mView.mGame.isActive) {
            mView.mGame.makeToast(R.string.message_unable_saving_on_game_over)
            return
        }
        val rows: Int = PrimaryMenuActivity.rows
        val CURRENT_STATE = "cs"
        val WIDTH = "width$rows$CURRENT_STATE"
        val HEIGHT = "height$rows$CURRENT_STATE"
        val UNDO_GRID = "undo$rows$CURRENT_STATE"
        val REWARD_DELETES = "reward chances$rows$CURRENT_STATE"
        val SCORE = "score$rows$CURRENT_STATE"
        val UNDO_SCORE = "undo score$rows$CURRENT_STATE"
        val CAN_UNDO = "can undo$rows$CURRENT_STATE"
        val GAME_STATE = "game state$rows$CURRENT_STATE"
        val UNDO_GAME_STATE = "undo game state$rows$CURRENT_STATE"
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        val field = mView.mGame.mGrid!!.mField
        val undoField = mView.mGame.mGrid!!.mUndoField
        editor.putInt(WIDTH, field.size)
        editor.putInt(HEIGHT, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                if (field[xx][yy] != null) editor.putInt(
                    "$CURRENT_STATE$rows $xx $yy",
                    field[xx][yy]!!.mValue
                ) else editor.putInt(
                    "$CURRENT_STATE$rows $xx $yy", 0
                )
                if (undoField[xx][yy] != null) editor.putInt(
                    "$UNDO_GRID$rows $xx $yy",
                    undoField[xx][yy]!!.mValue
                ) else editor.putInt(
                    "$UNDO_GRID$rows $xx $yy", 0
                )
            }
        }

        // reward deletions:
        editor.putInt(REWARD_DELETES, PrimaryActivity.mRewardDeletes)

        // game values:
        editor.putLong(SCORE, mView.mGame.mScore)
        editor.putLong(UNDO_SCORE, mView.mGame.mLastScore)
        editor.putBoolean(CAN_UNDO, mView.mGame.mCanUndo)
        editor.putInt(GAME_STATE, mView.mGame.mGameState)
        editor.putInt(UNDO_GAME_STATE, mView.mGame.mLastGameState)
        editor.putBoolean("has_snapshot$rows", true) // important
        editor.apply()
        mView.mGame.makeToast(R.string.saved)
    }

    companion object {
        const val SPAWN_ANIMATION = -1
        const val MOVE_ANIMATION = 0
        const val MERGE_ANIMATION = 1
        const val FADE_GLOBAL_ANIMATION = 0
        private const val MOVE_ANIMATION_TIME = PrimaryView.BASE_ANIMATION_TIME.toLong()
        private const val SPAWN_ANIMATION_TIME = PrimaryView.BASE_ANIMATION_TIME.toLong()
        private const val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME
        private const val NOTIFICATION_ANIMATION_TIME = (PrimaryView.BASE_ANIMATION_TIME * 5).toLong()
        private const val startingMaxValue = 2048

        //Odd state = game is not active
        //Even state = game is active
        //Win state = active state + 1
        private const val GAME_WIN = 1
        private const val GAME_LOST = -1
        private const val GAME_NORMAL = 0
        private const val GAME_ENDLESS = 2
        private const val GAME_ENDLESS_WON = 3
        private const val HIGH_SCORE = "high score"
        private var endingMaxValue: Int = 0
    }

    init {
        endingMaxValue = Math.pow(2.0, (mView.mNumCellTypes - 1).toDouble())
            .toInt()
    }
}