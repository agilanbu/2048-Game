package com.example.game2048agilanbu;

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.View
import java.lang.Exception

class MainView : View {
    @JvmField
    val numCellTypes = 21
    private val bitmapCell = arrayOfNulls<BitmapDrawable>(numCellTypes)
    @JvmField
    val game: PrimaryGame

    //Internal variables
    private val paint = Paint()
    @JvmField
    var hasSaveState = false
    @JvmField
    var continueButtonEnabled = false
    @JvmField
    var startingX = 0
    @JvmField
    var startingY = 0
    @JvmField
    var endingX = 0
    @JvmField
    var endingY = 0

    //Icons
    @JvmField
    var sYIcons = 0
    @JvmField
    var sXNewGame = 0
    @JvmField
    var sXUndo = 0
    @JvmField
    var sXRemoveTiles // trsh button
            = 0
    @JvmField
    var sXSave // save the current board
            = 0
    @JvmField
    var sXLoad // load previous saved board
            = 0
    @JvmField
    var iconSize = 0

    //Misc
    @JvmField
    var refreshLastTime = true

    //Timing
    private var lastFPSTime = System.nanoTime()

    //Text
    private var titleTextSize = 0f
    private var bodyTextSize = 0f
    private var headerTextSize = 0f
    private var gameOverTextSize = 0f

    //Layout variables
    private var cellSize = 0
    private var textSize = 0f
    private var cellTextSize = 0f
    private var gridWidth = 0
    private var textPaddingSize = 0
    private var iconPaddingSize = 0

    //Assets
    private var backgroundRectangle: Drawable? = null
    private var lightUpRectangle: Drawable? = null
    private var fadeRectangle: Drawable? = null
    private var background: Bitmap? = null
    private var loseGameOverlay: BitmapDrawable? = null
    private var winGameContinueOverlay: BitmapDrawable? = null
    private var winGameFinalOverlay: BitmapDrawable? = null

    //Text variables
    private var sYAll = 0
    private var titleStartYAll = 0
    private var bodyStartYAll = 0
    private var eYAll = 0
    private var titleWidthHighScore = 0
    private var titleWidthScore = 0
    var mActivity: PrimaryActivity? = null
    var mContext: Context

    constructor(context: Context, activity: PrimaryActivity?) : super(context) {
        mActivity = activity
        mContext = context

        //Loading resources
        game = PrimaryGame(context, this)
        try {
            //Getting assets
            backgroundRectangle = getDrawable(R.drawable.background_rectangle)
            lightUpRectangle = getDrawable(R.drawable.light_up_rectangle)
            fadeRectangle = getDrawable(R.drawable.fade_rectangle)
            setBackgroundColor(PrimaryMenuActivity.mBackgroundColor)
            val font = Typeface.createFromAsset(resources.assets, "ClearSans-Bold.ttf")
            paint.typeface = font
            paint.isAntiAlias = true
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assets?", e)
        }
        setOnTouchListener(GestureInputListener(this))
        game.newGame()
    }

    constructor(context: Context) : super(context) {
        mContext = context

        //Loading resources
        game = PrimaryGame(context, this)
        try {
            //Getting assets
            backgroundRectangle = getDrawable(R.drawable.background_rectangle)
            lightUpRectangle = getDrawable(R.drawable.light_up_rectangle)
            fadeRectangle = getDrawable(R.drawable.fade_rectangle)
            setBackgroundColor(PrimaryMenuActivity.mBackgroundColor)
            val font = Typeface.createFromAsset(resources.assets, "ClearSans-Bold.ttf")
            paint.typeface = font
            paint.isAntiAlias = true
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assets?", e)
        }
        setOnTouchListener(GestureInputListener(this))
        game.newGame()
    }

    public override fun onDraw(canvas: Canvas) {
        //Reset the transparency of the screen
        canvas.drawBitmap(background!!, 0f, 0f, paint)
        drawScoreText(canvas)
        drawCells(canvas)
        if (!game.canContinue()) drawEndlessText(canvas)

        // checking game over here
        if (!game.isActive) {
            drawEndGameState(canvas)
            if (!game.aGrid!!.isAnimationActive) drawGameOverButtons(canvas)
        }

        //Refresh the screen if there is still an animation running
        if (game.aGrid!!.isAnimationActive) {
            invalidate(startingX, startingY, endingX, endingY)
            tick()
            //Refresh one last time on game end.
        } else if (!game.isActive && refreshLastTime) {
            invalidate()
            refreshLastTime = false
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(width, height, oldW, oldH)
        getLayout(width, height)
        createBitmapCells()
        createBackgroundBitmap(width, height)
        createOverlays()
    }

    private fun getDrawable(resId: Int): Drawable {
        return resources.getDrawable(resId)
    }

    private fun drawDrawable(
        canvas: Canvas,
        draw: Drawable?,
        startingX: Int,
        startingY: Int,
        endingX: Int,
        endingY: Int
    ) {
        draw!!.setBounds(startingX, startingY, endingX, endingY)
        draw.draw(canvas)
    }

    private fun drawCellText(canvas: Canvas, value: Int) {
        val textShiftY = centerText()
        if (value >= 8) paint.color = resources.getColor(R.color.text_white) else paint.color =
            resources.getColor(R.color.text_black)
        canvas.drawText(
            "" + value,
            (cellSize / 2).toFloat(),
            (cellSize / 2 - textShiftY).toFloat(),
            paint
        )
    }

    private fun drawScoreText(canvas: Canvas) {
        //Drawing the score text: Ver 2
        paint.textSize = bodyTextSize
        paint.textAlign = Paint.Align.CENTER
        val bodyWidthHighScore = paint.measureText("" + game.highScore).toInt()
        val bodyWidthScore = paint.measureText("" + game.score).toInt()
        val textWidthHighScore =
            Math.max(titleWidthHighScore, bodyWidthHighScore) + textPaddingSize * 2
        val textWidthScore = Math.max(titleWidthScore, bodyWidthScore) + textPaddingSize * 2
        val textMiddleHighScore = textWidthHighScore / 2
        val textMiddleScore = textWidthScore / 2
        val eXHighScore = endingX
        val sXHighScore = eXHighScore - textWidthHighScore
        val eXScore = sXHighScore - textPaddingSize
        val sXScore = eXScore - textWidthScore

        //Outputting high-scores box
        backgroundRectangle!!.setBounds(sXHighScore, sYAll, eXHighScore, eYAll)
        backgroundRectangle!!.draw(canvas)
        paint.textSize = titleTextSize
        paint.color = resources.getColor(R.color.text_brown)
        canvas.drawText(
            resources.getString(R.string.high_score),
            (sXHighScore + textMiddleHighScore).toFloat(),
            titleStartYAll.toFloat(),
            paint
        )
        paint.textSize = bodyTextSize
        paint.color = resources.getColor(R.color.text_white)
        canvas.drawText(
            game.highScore.toString(),
            (sXHighScore + textMiddleHighScore).toFloat(),
            bodyStartYAll.toFloat(),
            paint
        )

        //Outputting scores box
        backgroundRectangle!!.setBounds(sXScore, sYAll, eXScore, eYAll)
        backgroundRectangle!!.draw(canvas)
        paint.textSize = titleTextSize
        paint.color = resources.getColor(R.color.text_brown)
        canvas.drawText(
            resources.getString(R.string.score),
            (sXScore + textMiddleScore).toFloat(),
            titleStartYAll.toFloat(),
            paint
        )
        paint.textSize = bodyTextSize
        paint.color = resources.getColor(R.color.text_white)
        canvas.drawText(
            game.score.toString(),
            (sXScore + textMiddleScore).toFloat(),
            bodyStartYAll.toFloat(),
            paint
        )
    }

    fun drawLoadButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, lightUpRectangle, sXLoad, sYIcons,
            sXLoad + iconSize,
            sYIcons + iconSize
        ) else drawDrawable(
            canvas, backgroundRectangle, sXLoad, sYIcons,
            sXLoad + iconSize,
            sYIcons + iconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_load),
            sXLoad + iconPaddingSize,
            sYIcons + iconPaddingSize,
            sXLoad + iconSize - iconPaddingSize,
            sYIcons + iconSize - iconPaddingSize
        )
    }

    fun drawSaveButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, lightUpRectangle, sXSave, sYIcons,
            sXSave + iconSize,
            sYIcons + iconSize
        ) else drawDrawable(
            canvas, backgroundRectangle, sXSave, sYIcons,
            sXSave + iconSize,
            sYIcons + iconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_save),
            sXSave + iconPaddingSize,
            sYIcons + iconPaddingSize,
            sXSave + iconSize - iconPaddingSize,
            sYIcons + iconSize - iconPaddingSize
        )
    }

    private fun drawTrashButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, lightUpRectangle, sXRemoveTiles, sYIcons,
            sXRemoveTiles + iconSize,
            sYIcons + iconSize
        ) else drawDrawable(
            canvas, backgroundRectangle, sXRemoveTiles, sYIcons,
            sXRemoveTiles + iconSize,
            sYIcons + iconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_trash),
            sXRemoveTiles + iconPaddingSize,
            sYIcons + iconPaddingSize,
            sXRemoveTiles + iconSize - iconPaddingSize,
            sYIcons + iconSize - iconPaddingSize
        )
    }

    private fun drawNewGameButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, lightUpRectangle, sXNewGame, sYIcons,
            sXNewGame + iconSize,
            sYIcons + iconSize
        ) else drawDrawable(
            canvas, backgroundRectangle, sXNewGame, sYIcons,
            sXNewGame + iconSize,
            sYIcons + iconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_refresh),
            sXNewGame + iconPaddingSize,
            sYIcons + iconPaddingSize,
            sXNewGame + iconSize - iconPaddingSize,
            sYIcons + iconSize - iconPaddingSize
        )
    }

    private fun drawUndoButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, lightUpRectangle, sXUndo, sYIcons,
            sXUndo + iconSize,
            sYIcons + iconSize
        ) else drawDrawable(
            canvas, backgroundRectangle, sXUndo, sYIcons,
            sXUndo + iconSize,
            sYIcons + iconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_undo),
            sXUndo + iconPaddingSize,
            sYIcons + iconPaddingSize,
            sXUndo + iconSize - iconPaddingSize,
            sYIcons + iconSize - iconPaddingSize
        )
    }

    private fun drawHeader(canvas: Canvas) {
        paint.textSize = headerTextSize
        paint.color = resources.getColor(R.color.text_black)
        paint.textAlign = Paint.Align.LEFT
        val textShiftY = centerText() * 2
        val headerStartY = sYAll - textShiftY
        canvas.drawText(
            resources.getString(R.string.header),
            startingX.toFloat(),
            headerStartY.toFloat(),
            paint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY)
    }

    //Renders the set of 16 background squares.
    private fun drawBackgroundGrid(canvas: Canvas) {
        val ROWS = PrimaryMenuActivity.rows
        val backgroundCell = getDrawable(R.drawable.cell_rectangle)
        // Outputting the game grid
        for (xx in 0 until ROWS) for (yy in 0 until ROWS) {
            val sX = startingX + gridWidth + (cellSize + gridWidth) * xx
            val eX = sX + cellSize
            val sY = startingY + gridWidth + (cellSize + gridWidth) * yy
            val eY = sY + cellSize
            drawDrawable(canvas, backgroundCell, sX, sY, eX, eY)
        }
    }

    private fun drawCells(canvas: Canvas) {
        val ROWS = PrimaryMenuActivity.rows
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        // Outputting the individual cells
        for (xx in 0 until ROWS) {
            for (yy in 0 until ROWS) {
                val sX = startingX + gridWidth + (cellSize + gridWidth) * xx
                val eX = sX + cellSize
                val sY = startingY + gridWidth + (cellSize + gridWidth) * yy
                val eY = sY + cellSize
                val currentTile = game.grid!!.getCellContent(xx, yy)
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    val value = currentTile.value
                    val index = log2(value)

                    //Check for any active animations
                    val aArray = game.aGrid!!.getAnimationCell(xx, yy)
                    var animated = false
                    for (i in aArray.indices.reversed()) {
                        val aCell = aArray[i]
                        //If this animation is not active, skip it
                        if (aCell.animationType == PrimaryGame.SPAWN_ANIMATION) animated = true
                        if (!aCell.isActive) continue
                        if (aCell.animationType == PrimaryGame.SPAWN_ANIMATION) // Spawning animation
                        {
                            val percentDone = aCell.percentageDone
                            val textScaleSize = percentDone.toFloat()
                            paint.textSize = textSize * textScaleSize
                            val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                            bitmapCell[index]!!.setBounds(
                                (sX + cellScaleSize).toInt(),
                                (sY + cellScaleSize).toInt(),
                                (eX - cellScaleSize).toInt(),
                                (eY - cellScaleSize).toInt()
                            )
                            bitmapCell[index]!!.draw(canvas)
                        } else if (aCell.animationType == PrimaryGame.MERGE_ANIMATION) // Merging Animation
                        {
                            val percentDone = aCell.percentageDone
                            val textScaleSize =
                                (1 + INITIAL_VELOCITY * percentDone + MERGING_ACCELERATION * percentDone * percentDone / 2).toFloat()
                            paint.textSize = textSize * textScaleSize
                            val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                            bitmapCell[index]!!.setBounds(
                                (sX + cellScaleSize).toInt(),
                                (sY + cellScaleSize).toInt(),
                                (eX - cellScaleSize).toInt(),
                                (eY - cellScaleSize).toInt()
                            )
                            bitmapCell[index]!!.draw(canvas)
                        } else if (aCell.animationType == PrimaryGame.MOVE_ANIMATION) // Moving animation
                        {
                            val percentDone = aCell.percentageDone
                            var tempIndex = index
                            if (aArray.size >= 2) tempIndex = tempIndex - 1
                            val previousX = aCell.mExtras[0]
                            val previousY = aCell.mExtras[1]
                            val currentX = currentTile.x
                            val currentY = currentTile.y
                            val dX =
                                ((currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1) * 1.0).toInt()
                            val dY =
                                ((currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1) * 1.0).toInt()
                            bitmapCell[tempIndex]!!.setBounds(sX + dX, sY + dY, eX + dX, eY + dY)
                            bitmapCell[tempIndex]!!.draw(canvas)
                        }
                        animated = true
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        bitmapCell[index]!!.setBounds(sX, sY, eX, eY)
                        bitmapCell[index]!!.draw(canvas)
                    }
                }
            }
        }
    }

    private fun drawEndGameState(canvas: Canvas) {
        var alphaChange = 1.0
        continueButtonEnabled = false
        for (animation in game.aGrid!!.mGlobalAnimation) if (animation.animationType == PrimaryGame.FADE_GLOBAL_ANIMATION) alphaChange =
            animation.percentageDone
        var displayOverlay: BitmapDrawable? = null
        if (game.gameWon()) {
            if (game.canContinue()) {
                continueButtonEnabled = true
                displayOverlay = winGameContinueOverlay
            } else displayOverlay = winGameFinalOverlay
        } else if (game.gameLost()) displayOverlay = loseGameOverlay
        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY)
            displayOverlay.alpha = (255 * alphaChange).toInt()
            displayOverlay.draw(canvas)
        }
    }

    private fun drawEndlessText(canvas: Canvas) {
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = bodyTextSize
        paint.color = resources.getColor(R.color.text_black)
        canvas.drawText(
            resources.getString(R.string.endless),
            startingX.toFloat(),
            (sYIcons - centerText() * 2).toFloat(),
            paint
        )
    }

    private fun drawGameOverButtons(canvas: Canvas) {
        drawNewGameButton(canvas, true)
        drawTrashButton(
            canvas,
            if (!game.gameWon() && PrimaryActivity.mRewardDeletes > 0) true else false
        )
        drawUndoButton(canvas, true)
        drawLoadButton(canvas, true)
        drawSaveButton(canvas, false)
    }

    private fun createEndGameStates(canvas: Canvas, win: Boolean, showButton: Boolean) {
        val width = endingX - startingX
        val length = endingY - startingY
        val middleX = width / 2
        val middleY = length / 2
        if (win) {
            lightUpRectangle!!.alpha = 127
            drawDrawable(canvas, lightUpRectangle, 0, 0, width, length)
            lightUpRectangle!!.alpha = 255
            paint.color = resources.getColor(R.color.text_white)
            paint.alpha = 255
            paint.textSize = gameOverTextSize
            paint.textAlign = Paint.Align.CENTER
            val textBottom = middleY - centerText()
            canvas.drawText(
                resources.getString(R.string.you_win),
                middleX.toFloat(),
                textBottom.toFloat(),
                paint
            )
            paint.textSize = bodyTextSize
            val text =
                if (showButton) resources.getString(R.string.go_on) else resources.getString(R.string.for_now)
            canvas.drawText(
                text,
                middleX.toFloat(),
                (textBottom + textPaddingSize * 2 - centerText() * 2).toFloat(),
                paint
            )
        } else {
            fadeRectangle!!.alpha = 127
            drawDrawable(canvas, fadeRectangle, 0, 0, width, length)
            fadeRectangle!!.alpha = 255
            paint.color = resources.getColor(R.color.text_black)
            paint.alpha = 255
            paint.textSize = gameOverTextSize
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                resources.getString(R.string.game_over),
                middleX.toFloat(),
                (middleY - centerText()).toFloat(),
                paint
            )
        }
    }

    private fun createBackgroundBitmap(width: Int, height: Int) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(background!!)
        drawHeader(canvas)
        drawNewGameButton(canvas, false)
        drawUndoButton(canvas, false)
        drawTrashButton(canvas, false)
        drawLoadButton(canvas, true) // if checking are there a save state or no, is better!
        drawSaveButton(canvas, true)
        drawBackground(canvas)
        drawBackgroundGrid(canvas)
    }

    private fun createBitmapCells() {
        val resources = resources
        val cellRectangleIds = cellRectangleIds
        paint.textAlign = Paint.Align.CENTER
        for (xx in 1 until bitmapCell.size) {
            val value = Math.pow(2.0, xx.toDouble()).toInt()
            paint.textSize = cellTextSize
            val tempTextSize = cellTextSize * cellSize * 0.9f / Math.max(
                cellSize * 0.9f,
                paint.measureText(value.toString())
            )
            paint.textSize = tempTextSize
            val bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawDrawable(canvas, getDrawable(cellRectangleIds[xx]), 0, 0, cellSize, cellSize)
            drawCellText(canvas, value)
            bitmapCell[xx] = BitmapDrawable(resources, bitmap)
        }
    }

    private val cellRectangleIds: IntArray
        private get() {
            val cellRectangleIds = IntArray(numCellTypes)
            cellRectangleIds[0] = R.drawable.cell_rectangle
            cellRectangleIds[1] = R.drawable.cell_rectangle_2
            cellRectangleIds[2] = R.drawable.cell_rectangle_4
            cellRectangleIds[3] = R.drawable.cell_rectangle_8
            cellRectangleIds[4] = R.drawable.cell_rectangle_16
            cellRectangleIds[5] = R.drawable.cell_rectangle_32
            cellRectangleIds[6] = R.drawable.cell_rectangle_64
            cellRectangleIds[7] = R.drawable.cell_rectangle_128
            cellRectangleIds[8] = R.drawable.cell_rectangle_256
            cellRectangleIds[9] = R.drawable.cell_rectangle_512
            cellRectangleIds[10] = R.drawable.cell_rectangle_1024
            cellRectangleIds[11] = R.drawable.cell_rectangle_2048
            for (xx in 12 until cellRectangleIds.size) cellRectangleIds[xx] =
                R.drawable.cell_rectangle_4096
            return cellRectangleIds
        }

    private fun createOverlays() {
        val resources = resources
        //Initialize overlays
        var bitmap =
            Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap!!)
        createEndGameStates(canvas, true, true)
        winGameContinueOverlay = BitmapDrawable(resources, bitmap)
        bitmap =
            Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, true, false)
        winGameFinalOverlay = BitmapDrawable(resources, bitmap)
        bitmap =
            Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, false, false)
        loseGameOverlay = BitmapDrawable(resources, bitmap)
    }

    private fun tick() {
        val currentTime = System.nanoTime()
        game.aGrid!!.tickAll(currentTime - lastFPSTime)
        lastFPSTime = currentTime
    }

    fun resyncTime() {
        lastFPSTime = System.nanoTime()
    }

    private fun getLayout(width: Int, height: Int) {
        val ROWS = PrimaryMenuActivity.rows
        cellSize = Math.min(width / (ROWS + 1), height / (ROWS + 3))
        gridWidth = cellSize / (ROWS + 3) // (ROWS + 3) was 7
        val screenMiddleX = width / 2
        val screenMiddleY = height / 2
        val boardMiddleY = screenMiddleY + cellSize / 2
        iconSize = cellSize / 2

        //Grid Dimensions
        val halfNumSquaresX = ROWS / 2.0
        val halfNumSquaresY = ROWS / 2.0
        startingX =
            (screenMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2).toInt()
        endingX = (screenMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2).toInt()
        startingY =
            (boardMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2).toInt()
        endingY = (boardMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2).toInt()
        val widthWithPadding = (endingX - startingX).toFloat()

        // Text Dimensions
        paint.textSize = cellSize.toFloat()
        textSize = cellSize * cellSize / Math.max(cellSize.toFloat(), paint.measureText("0000"))
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 1000f
        gameOverTextSize = Math.min(
            Math.min(
                1000f * ((widthWithPadding - gridWidth * 2) / paint.measureText(
                    resources.getString(
                        R.string.game_over
                    )
                )),
                textSize * 2
            ),
            1000f * ((widthWithPadding - gridWidth * 2) / paint.measureText(resources.getString(R.string.you_win)))
        )
        paint.textSize = cellSize.toFloat()
        cellTextSize = textSize
        titleTextSize = textSize / 3
        bodyTextSize = (textSize / 1.5).toFloat()
        headerTextSize = textSize * 2
        textPaddingSize = (textSize / 3).toInt()
        iconPaddingSize = (textSize / 5).toInt()
        paint.textSize = titleTextSize
        var textShiftYAll = centerText()
        //static variables
        sYAll = (startingY - cellSize * 1.5).toInt()
        titleStartYAll = (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll).toInt()
        bodyStartYAll =
            (titleStartYAll + textPaddingSize + titleTextSize / 2 + bodyTextSize / 2).toInt()
        titleWidthHighScore = paint.measureText(resources.getString(R.string.high_score)).toInt()
        titleWidthScore = paint.measureText(resources.getString(R.string.score)).toInt()
        paint.textSize = bodyTextSize
        textShiftYAll = centerText()
        eYAll = (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize).toInt()
        sYIcons = (startingY + eYAll) / 2 - iconSize / 2
        sXNewGame = endingX - iconSize
        sXUndo = sXNewGame - iconSize - iconPaddingSize
        sXRemoveTiles = sXUndo - iconSize - iconPaddingSize
        sXLoad = sXRemoveTiles - iconSize - iconPaddingSize
        sXSave = sXLoad - iconSize - iconPaddingSize
        resyncTime()
    }

    private fun centerText(): Int {
        return ((paint.descent() + paint.ascent()) / 2).toInt()
    }

    companion object {
        //Internal Constants
        const val BASE_ANIMATION_TIME = 100000000
        private val TAG = MainView::class.java.simpleName
        private const val MERGING_ACCELERATION = (-0.5).toFloat()
        private const val INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4
        private fun log2(n: Int): Int {
            require(n > 0)
            return 31 - Integer.numberOfLeadingZeros(n)
        }
    }
}