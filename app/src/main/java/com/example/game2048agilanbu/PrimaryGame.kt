package com.example.game2048agilanbu

import android.app.AlertDialog
import android.content.Context
import android.preference.PreferenceManager
import android.widget.Toast
import java.util.*

class PrimaryGame(private val mContext: Context, private val mView: MainView) {
    @JvmField
    var gameState = GAME_NORMAL
    @JvmField
    var lastGameState = GAME_NORMAL
    private var bufferGameState = GAME_NORMAL
    @JvmField
    var grid: GridItems? = null
    @JvmField
    var aGrid: AnimGridItems? = null
    @JvmField
    var canUndo = false
    @JvmField
    var score: Long = 0
    @JvmField
    var highScore: Long = 0
    @JvmField
    var lastScore: Long = 0
    private var bufferScore: Long = 0
    fun newGame() {
        val rows: Int = PrimaryMenuActivity.rows
        if (grid == null) grid = GridItems(rows, rows) else {
            prepareUndoState()
            saveUndoState()
            grid!!.clearGrid()
        }
        aGrid = AnimGridItems(rows, rows)
        highScore = getHighScore()
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
        score = 0
        gameState = GAME_NORMAL
        addStartTiles()
        mView.refreshLastTime = true
        mView.resyncTime()
        mView.invalidate()
    }

    private fun addStartTiles() {
        val startTiles = 2
        for (xx in 0 until startTiles) addRandomTile()
    }

    private fun addRandomTile() {
        if (grid!!.isCellsAvailable) {
            val value = if (Math.random() < 0.9) 2 else 4
            val tile = GridTiles(grid!!.randomAvailableCell()!!, value)
            spawnTile(tile)
        }
    }

    private fun spawnTile(tile: GridTiles) {
        grid!!.insertTile(tile)
        aGrid!!.startAnimation(
            tile.x, tile.y, SPAWN_ANIMATION,
            SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null
        ) //Direction: -1 = EXPANDING
    }

    private fun recordHighScore() {
        val rows: Int = PrimaryMenuActivity.rows
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        editor.putLong(HIGH_SCORE + rows, highScore)
        editor.apply()
    }

    private fun getHighScore(): Long {
        val rows: Int = PrimaryMenuActivity.rows
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        return settings.getLong(HIGH_SCORE + rows, -1)
    }

    private fun prepareTiles() {
        for (array in grid!!.field) for (tile in array) if (grid!!.isCellOccupied(tile)) tile!!.mergedFrom =
            null
    }

    private fun moveTile(tile: GridTiles, cell: CellItem) {
        grid!!.field[tile.x][tile.y] = null
        grid!!.field[cell.x][cell.y] = tile
        tile.updatePosition(cell)
    }

    private fun saveUndoState() {
        grid!!.saveTiles()
        canUndo = true
        lastScore = bufferScore
        lastGameState = bufferGameState
    }

    private fun prepareUndoState() {
        grid!!.prepareSaveTiles()
        bufferScore = score
        bufferGameState = gameState
    }

    fun revertUndoState() {
        if (canUndo) {
            canUndo = false
            aGrid!!.cancelAnimations()
            grid!!.revertTiles()
            score = lastScore
            gameState = lastGameState
            mView.refreshLastTime = true
            mView.invalidate()
        }
    }

    fun gameWon(): Boolean {
        return gameState > 0 && gameState % 2 != 0
    }

    fun gameLost(): Boolean {
        return gameState == GAME_LOST
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
                val tile = grid!!.getCellContent(cell)
                if (tile != null) {
                    val positions = findFarthestPosition(cell, vector)
                    val next = grid!!.getCellContent(positions[1])
                    if (next != null && next.value == tile.value && next.mergedFrom == null) {
                        val merged = GridTiles(positions[1], tile.value * 2)
                        val temp = arrayOf(tile, next)
                        merged.mergedFrom = temp
                        grid!!.insertTile(merged)
                        grid!!.removeTile(tile)

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
                        score = score + merged.value
                        highScore = Math.max(score, highScore)

                        // The mighty 2048 tile
                        if (merged.value >= winValue() && !gameWon()) {
                            gameState = gameState + GAME_WIN // Set win state
                            endGame()
                        }
                        if (!PrimaryMenuActivity.mIsMainMenu) {
                            if (merged.value >= 32) {
                                PrimaryActivity.unlockAchievement(merged.value)
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
            gameState = GAME_LOST
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
        if (score >= highScore) {
            highScore = score
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
        } while (grid!!.isCellWithinBounds(nextCell) && grid!!.isCellAvailable(nextCell))
        return arrayOf(previous, nextCell)
    }

    private fun movesAvailable(): Boolean {
        return grid!!.isCellsAvailable || tileMatchesAvailable()
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: GridTiles?
        val rows: Int = PrimaryMenuActivity.rows
        for (xx in 0 until rows) {
            for (yy in 0 until rows) {
                tile = grid!!.getCellContent(CellItem(xx, yy))
                if (tile != null) {
                    for (direction in 0..3) {
                        val vector = getVector(direction)
                        val cell = CellItem(xx + vector.x, yy + vector.y)
                        val other = grid!!.getCellContent(cell)
                        if (other != null && other.value == tile.value) return true
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
        gameState = GAME_ENDLESS
        mView.invalidate()
        mView.refreshLastTime = true
    }

    fun canContinue(): Boolean {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON)
    }

    private fun customSaveLoadTemp() {
        val rows: Int = PrimaryMenuActivity.rows
        val WIDTH = "width" + rows + "temp"
        val HEIGHT = "height" + rows + "temp"
        var deleteAmount = rows - 1

        // Save() as "temp"
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        val field = grid!!.field
        editor.putInt(WIDTH, field.size)
        editor.putInt(HEIGHT, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                if (field[xx][yy] != null) {
                    if (field[xx][yy]!!.value >= 2 && field[xx][yy]!!.value <= 32 && deleteAmount > 0) {
                        deleteAmount--
                        editor.putInt(rows.toString() + " " + xx + " " + yy + "temp", 0)
                    } else editor.putInt(
                        rows.toString() + " " + xx + " " + yy + "temp",
                        field[xx][yy]!!.value
                    )
                } else editor.putInt(rows.toString() + " " + xx + " " + yy + "temp", 0)
            }
        }
        editor.apply()

        // Load() as "temp"
        for (xx in grid!!.field.indices) {
            for (yy in 0 until grid!!.field[0].size) {
                val value = settings.getInt(rows.toString() + " " + xx + " " + yy + "temp", -1)
                if (value > 0) grid!!.field[xx][yy] =
                    GridTiles(xx, yy, value) else if (value == 0) grid!!.field[xx][yy] = null
            }
        }
        canUndo = false
        gameState = lastGameState
    }

    fun makeToast(resId: Int) {
        Toast.makeText(mContext, mContext.getString(resId), Toast.LENGTH_SHORT).show()
    }

    fun RemoveTilesWithTrash() {
        val rows: Int = PrimaryMenuActivity.rows
        val cellCount = rows * rows - (rows + 2)
        if (mContext.javaClass == ColorPlatActivity::class.java) // because of color picker
        {
            if (mView.game.grid!!.getAvailableCells().size < cellCount) {
                customSaveLoadTemp()
                mView.invalidate()
            } else mView.game.makeToast(R.string.tiles_are_not_enough_to_remove)
        } else {
            if (PrimaryActivity.mRewardDeletes > 0) {
                if (mView.game.grid!!.getAvailableCells().size < cellCount) {
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
                } else mView.game.makeToast(R.string.tiles_are_not_enough_to_remove)
            } else mView.game.makeToast(R.string.reward_amount_error)
        }
    }

    fun loadCurrentBoard() {
        //Stopping all animations
        mView.game.aGrid!!.cancelAnimations()
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val rows: Int = PrimaryMenuActivity.rows
        if (!settings.getBoolean("has_snapshot$rows", false)) {
            // if we haven't any snapshot already, so we better don't loading anything
            mView.game.makeToast(R.string.loading_failed)
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
        for (xx in mView.game.grid!!.field.indices) {
            for (yy in 0 until mView.game.grid!!.field[0].size) {
                val value = settings.getInt("$CURRENT_STATE$rows $xx $yy", -1)
                if (value > 0) mView.game.grid!!.field[xx][yy] =
                    GridTiles(xx, yy, value) else if (value == 0) mView.game.grid!!.field[xx][yy] = null
                val undoValue = settings.getInt("$UNDO_GRID$rows $xx $yy", -1)
                if (undoValue > 0) mView.game.grid!!.undoField[xx][yy] = GridTiles(
                    xx,
                    yy,
                    undoValue
                ) else if (value == 0) mView.game.grid!!.undoField[xx][yy] = null
            }
        }
        PrimaryActivity.mRewardDeletes = settings.getInt(REWARD_DELETES, 2)
        mView.game.score = settings.getLong(SCORE, mView.game.score)
        mView.game.highScore = settings.getLong(HIGH_SCORE, mView.game.highScore)
        mView.game.lastScore = settings.getLong(UNDO_SCORE, mView.game.lastScore)
        mView.game.canUndo = settings.getBoolean(CAN_UNDO, mView.game.canUndo)
        mView.game.gameState = settings.getInt(GAME_STATE, mView.game.gameState)
        mView.game.lastGameState = settings.getInt(UNDO_GAME_STATE, mView.game.lastGameState)
        mView.invalidate()
        mView.game.makeToast(R.string.loaded)
    }

    fun saveCurrentBoard() {
        if (!mView.game.isActive) {
            mView.game.makeToast(R.string.message_unable_saving_on_game_over)
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
        val field = mView.game.grid!!.field
        val undoField = mView.game.grid!!.undoField
        editor.putInt(WIDTH, field.size)
        editor.putInt(HEIGHT, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                if (field[xx][yy] != null) editor.putInt(
                    "$CURRENT_STATE$rows $xx $yy",
                    field[xx][yy]!!.value
                ) else editor.putInt(
                    "$CURRENT_STATE$rows $xx $yy", 0
                )
                if (undoField[xx][yy] != null) editor.putInt(
                    "$UNDO_GRID$rows $xx $yy",
                    undoField[xx][yy]!!.value
                ) else editor.putInt(
                    "$UNDO_GRID$rows $xx $yy", 0
                )
            }
        }

        // reward deletions:
        editor.putInt(REWARD_DELETES, PrimaryActivity.mRewardDeletes)

        // game values:
        editor.putLong(SCORE, mView.game.score)
        editor.putLong(UNDO_SCORE, mView.game.lastScore)
        editor.putBoolean(CAN_UNDO, mView.game.canUndo)
        editor.putInt(GAME_STATE, mView.game.gameState)
        editor.putInt(UNDO_GAME_STATE, mView.game.lastGameState)
        editor.putBoolean("has_snapshot$rows", true) // important
        editor.apply()
        mView.game.makeToast(R.string.saved)
    }

    companion object {
        const val SPAWN_ANIMATION = -1
        const val MOVE_ANIMATION = 0
        const val MERGE_ANIMATION = 1
        const val FADE_GLOBAL_ANIMATION = 0
        private const val MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME.toLong()
        private const val SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME.toLong()
        private const val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME
        private const val NOTIFICATION_ANIMATION_TIME = (MainView.BASE_ANIMATION_TIME * 5).toLong()
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
        endingMaxValue = Math.pow(2.0, (mView.numCellTypes - 1).toDouble())
            .toInt()
    }
}