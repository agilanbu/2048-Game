package com.agilanbu.game2048kotlin;

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import java.lang.Exception

class PrimaryView : View {
    @JvmField
    val mNumCellTypes = 21
    private val mBitmapCell = arrayOfNulls<BitmapDrawable>(mNumCellTypes)

    @JvmField
    val mGame: PrimaryGame

    //Internal variables
    private val mPaint = Paint()

    @JvmField
    var mHasSaveState = false

    @JvmField
    var mContinueButtonEnabled = false

    @JvmField
    var mStartingX = 0

    @JvmField
    var mStartingY = 0

    @JvmField
    var mEndingX = 0

    @JvmField
    var mEndingY = 0

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
    var mIconSize = 0

    //Misc
    @JvmField
    var mRefreshLastTime = true

    //Timing
    private var mLastFPSTime = System.nanoTime()

    //Text
    private var mTitleTextSize = 0f
    private var mBodyTextSize = 0f
    private var mHeaderTextSize = 0f
    private var mGameOverTextSize = 0f

    //Layout variables
    private var mCellSize = 0
    private var mTextSize = 0f
    private var mCellTextSize = 0f
    private var mGridWidth = 0
    private var mTextPaddingSize = 0
    private var mIconPaddingSize = 0

    //Assets
    private var mBackgroundRectangle: Drawable? = null
    private var mLightUpRectangle: Drawable? = null
    private var mFadeRectangle: Drawable? = null
    private var mBackground: Bitmap? = null
    private var mLoseGameOverlay: BitmapDrawable? = null
    private var mWinGameContinueOverlay: BitmapDrawable? = null
    private var mWinGameFinalOverlay: BitmapDrawable? = null

    //Text variables
    private var sYAll = 0
    private var mTitleStartYAll = 0
    private var mBodyStartYAll = 0
    private var eYAll = 0
    private var mTitleWidthHighScore = 0
    private var mTitleWidthScore = 0
    var mActivity: PrimaryActivity? = null
    var mContext: Context

    constructor(context: Context, activity: PrimaryActivity?) : super(context) {
        mActivity = activity
        mContext = context

        //Loading resources
        mGame = PrimaryGame(context, this)
        try { //Getting assets
            mBackgroundRectangle = getDrawable(R.drawable.background_rectangle)
            mLightUpRectangle = getDrawable(R.drawable.light_up_rectangle)
            mFadeRectangle = getDrawable(R.drawable.fade_rectangle)
            setBackgroundColor(PrimaryMenuActivity.mBackgroundColor)
            val font = ResourcesCompat.getFont(mContext, R.font.pcsenior)
//            val font = Typeface.createFromAsset(resources.assets, "ClearSans-Bold.ttf")
            mPaint.typeface = font
            mPaint.isAntiAlias = true
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assets?", e)
        }
        setOnTouchListener(GestureInputListener(this))
        mGame.newGame()
    }

    constructor(context: Context) : super(context) {
        mContext = context

        //Loading resources
        mGame = PrimaryGame(context, this)
        try {
            //Getting assets
            mBackgroundRectangle = getDrawable(R.drawable.background_rectangle)
            mLightUpRectangle = getDrawable(R.drawable.light_up_rectangle)
            mFadeRectangle = getDrawable(R.drawable.fade_rectangle)
            setBackgroundColor(PrimaryMenuActivity.mBackgroundColor)
            val font = ResourcesCompat.getFont(mContext, R.font.pcsenior)
//            val font = ResourcesCompat.getFont(mContext, R.font.opensans_semibold)
            mPaint.typeface = font
            mPaint.isAntiAlias = true
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assets?", e)
        }
        setOnTouchListener(GestureInputListener(this))
        mGame.newGame()
    }

    public override fun onDraw(canvas: Canvas) {
        //Reset the transparency of the screen
        canvas.drawBitmap(mBackground!!, 0f, 0f, mPaint)
        drawScoreText(canvas)
        drawCells(canvas)
        if (!mGame.canContinue()) drawEndlessText(canvas)

        // checking game over here
        if (!mGame.isActive) {
            drawEndGameState(canvas)
            if (!mGame.aGrid!!.isAnimationActive) drawGameOverButtons(canvas)
        }

        //Refresh the screen if there is still an animation running
        if (mGame.aGrid!!.isAnimationActive) {
            invalidate(mStartingX, mStartingY, mEndingX, mEndingY)
            tick()
            //Refresh one last time on game end.
        } else if (!mGame.isActive && mRefreshLastTime) {
            invalidate()
            mRefreshLastTime = false
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
        if (value >= 8) mPaint.color = resources.getColor(R.color.text_white) else mPaint.color =
            resources.getColor(R.color.text_white)
        canvas.drawText(
            "" + value,
            (mCellSize / 2).toFloat(),
            (mCellSize / 2 - textShiftY).toFloat(),
            mPaint
        )
    }

    private fun drawScoreText(canvas: Canvas) {
        //Drawing the score text: Ver 2
        mPaint.textSize = mBodyTextSize
        mPaint.textAlign = Paint.Align.CENTER
        val bodyWidthHighScore = mPaint.measureText("" + mGame.mHighScore).toInt()
        val bodyWidthScore = mPaint.measureText("" + mGame.mScore).toInt()
        val textWidthHighScore =
            Math.max(mTitleWidthHighScore, bodyWidthHighScore) + mTextPaddingSize * 2
        val textWidthScore = Math.max(mTitleWidthScore, bodyWidthScore) + mTextPaddingSize * 2
        val textMiddleHighScore = textWidthHighScore / 2
        val textMiddleScore = textWidthScore / 2
        val eXHighScore = mEndingX
        val sXHighScore = eXHighScore - textWidthHighScore
        val eXScore = sXHighScore - mTextPaddingSize
        val sXScore = eXScore - textWidthScore

        //Outputting high-scores box
        mBackgroundRectangle!!.setBounds(sXHighScore, sYAll, eXHighScore, eYAll)
        mBackgroundRectangle!!.draw(canvas)
        mPaint.textSize = mTitleTextSize
        mPaint.color = resources.getColor(R.color.text_white)
        canvas.drawText(
            resources.getString(R.string.high_score),
            (sXHighScore + textMiddleHighScore).toFloat(),
            mTitleStartYAll.toFloat(),
            mPaint
        )
        mPaint.textSize = mBodyTextSize
        mPaint.color = resources.getColor(R.color.text_white)
        canvas.drawText(
            mGame.mHighScore.toString(),
            (sXHighScore + textMiddleHighScore).toFloat(),
            mBodyStartYAll.toFloat(),
            mPaint
        )

        //Outputting scores box
        mBackgroundRectangle!!.setBounds(sXScore, sYAll, eXScore, eYAll)
        mBackgroundRectangle!!.draw(canvas)
        mPaint.textSize = mTitleTextSize
        mPaint.color = resources.getColor(R.color.text_white)
        canvas.drawText(
            resources.getString(R.string.score),
            (sXScore + textMiddleScore).toFloat(),
            mTitleStartYAll.toFloat(),
            mPaint
        )
        mPaint.textSize = mBodyTextSize
        mPaint.color = resources.getColor(R.color.text_white)
        canvas.drawText(
            mGame.mScore.toString(),
            (sXScore + textMiddleScore).toFloat(),
            mBodyStartYAll.toFloat(),
            mPaint
        )
    }

    fun drawLoadButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, mLightUpRectangle, sXLoad, sYIcons,
            sXLoad + mIconSize,
            sYIcons + mIconSize
        ) else drawDrawable(
            canvas, mBackgroundRectangle, sXLoad, sYIcons,
            sXLoad + mIconSize,
            sYIcons + mIconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_load),
            sXLoad + mIconPaddingSize,
            sYIcons + mIconPaddingSize,
            sXLoad + mIconSize - mIconPaddingSize,
            sYIcons + mIconSize - mIconPaddingSize
        )
    }

    fun drawSaveButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, mLightUpRectangle, sXSave, sYIcons,
            sXSave + mIconSize,
            sYIcons + mIconSize
        ) else drawDrawable(
            canvas, mBackgroundRectangle, sXSave, sYIcons,
            sXSave + mIconSize,
            sYIcons + mIconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_save),
            sXSave + mIconPaddingSize,
            sYIcons + mIconPaddingSize,
            sXSave + mIconSize - mIconPaddingSize,
            sYIcons + mIconSize - mIconPaddingSize
        )
    }

    private fun drawTrashButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, mLightUpRectangle, sXRemoveTiles, sYIcons,
            sXRemoveTiles + mIconSize,
            sYIcons + mIconSize
        ) else drawDrawable(
            canvas, mBackgroundRectangle, sXRemoveTiles, sYIcons,
            sXRemoveTiles + mIconSize,
            sYIcons + mIconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_trash),
            sXRemoveTiles + mIconPaddingSize,
            sYIcons + mIconPaddingSize,
            sXRemoveTiles + mIconSize - mIconPaddingSize,
            sYIcons + mIconSize - mIconPaddingSize
        )
    }

    private fun drawNewGameButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, mLightUpRectangle, sXNewGame, sYIcons,
            sXNewGame + mIconSize,
            sYIcons + mIconSize
        ) else drawDrawable(
            canvas, mBackgroundRectangle, sXNewGame, sYIcons,
            sXNewGame + mIconSize,
            sYIcons + mIconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_refresh),
            sXNewGame + mIconPaddingSize,
            sYIcons + mIconPaddingSize,
            sXNewGame + mIconSize - mIconPaddingSize,
            sYIcons + mIconSize - mIconPaddingSize
        )
    }

    private fun drawUndoButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) drawDrawable(
            canvas, mLightUpRectangle, sXUndo, sYIcons,
            sXUndo + mIconSize,
            sYIcons + mIconSize
        ) else drawDrawable(
            canvas, mBackgroundRectangle, sXUndo, sYIcons,
            sXUndo + mIconSize,
            sYIcons + mIconSize
        )
        drawDrawable(
            canvas, getDrawable(R.drawable.ic_action_undo),
            sXUndo + mIconPaddingSize,
            sYIcons + mIconPaddingSize,
            sXUndo + mIconSize - mIconPaddingSize,
            sYIcons + mIconSize - mIconPaddingSize
        )
    }

    private fun drawHeader(canvas: Canvas) {
        mPaint.textSize = mHeaderTextSize
        mPaint.color = resources.getColor(R.color.text_white)
        mPaint.textAlign = Paint.Align.LEFT
        val textShiftY = centerText() * 2
        val headerStartY = sYAll - textShiftY
        canvas.drawText(
            resources.getString(R.string.header),
            mStartingX.toFloat(),
            headerStartY.toFloat(),
            mPaint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        drawDrawable(canvas, mBackgroundRectangle, mStartingX, mStartingY, mEndingX, mEndingY)
    }

    //Renders the set of 16 background squares.
    private fun drawBackgroundGrid(canvas: Canvas) {
        val ROWS = PrimaryMenuActivity.rows
        val backgroundCell = getDrawable(R.drawable.gridcell_rectangle)
        // Outputting the game grid
        for (xx in 0 until ROWS) for (yy in 0 until ROWS) {
            val sX = mStartingX + mGridWidth + (mCellSize + mGridWidth) * xx
            val eX = sX + mCellSize
            val sY = mStartingY + mGridWidth + (mCellSize + mGridWidth) * yy
            val eY = sY + mCellSize
            drawDrawable(canvas, backgroundCell, sX, sY, eX, eY)
        }
    }

    private fun drawCells(canvas: Canvas) {
        val ROWS = PrimaryMenuActivity.rows
        mPaint.textSize = mTextSize
        mPaint.textAlign = Paint.Align.CENTER
        // Outputting the individual cells
        for (xx in 0 until ROWS) {
            for (yy in 0 until ROWS) {
                val sX = mStartingX + mGridWidth + (mCellSize + mGridWidth) * xx
                val eX = sX + mCellSize
                val sY = mStartingY + mGridWidth + (mCellSize + mGridWidth) * yy
                val eY = sY + mCellSize
                val currentTile = mGame.mGrid!!.getCellContent(xx, yy)
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    val value = currentTile.mValue
                    val index = log2(value)

                    //Check for any active animations
                    val aArray = mGame.aGrid!!.getAnimationCell(xx, yy)
                    var animated = false
                    for (i in aArray.indices.reversed()) {
                        val aCell = aArray[i]
                        //If this animation is not active, skip it
                        if (aCell.getmAnimationType() == PrimaryGame.SPAWN_ANIMATION) animated =
                            true
                        if (!aCell.isActive) continue
                        if (aCell.getmAnimationType() == PrimaryGame.SPAWN_ANIMATION) // Spawning animation
                        {
                            val percentDone = aCell.percentageDone
                            val textScaleSize = percentDone.toFloat()
                            mPaint.textSize = mTextSize * textScaleSize
                            val cellScaleSize = mCellSize / 2 * (1 - textScaleSize)
                            mBitmapCell[index]!!.setBounds(
                                (sX + cellScaleSize).toInt(),
                                (sY + cellScaleSize).toInt(),
                                (eX - cellScaleSize).toInt(),
                                (eY - cellScaleSize).toInt()
                            )
                            mBitmapCell[index]!!.draw(canvas)
                        } else if (aCell.getmAnimationType() == PrimaryGame.MERGE_ANIMATION) // Merging Animation
                        {
                            val percentDone = aCell.percentageDone
                            val textScaleSize =
                                (1 + INITIAL_VELOCITY * percentDone + MERGING_ACCELERATION * percentDone * percentDone / 2).toFloat()
                            mPaint.textSize = mTextSize * textScaleSize
                            val cellScaleSize = mCellSize / 2 * (1 - textScaleSize)
                            mBitmapCell[index]!!.setBounds(
                                (sX + cellScaleSize).toInt(),
                                (sY + cellScaleSize).toInt(),
                                (eX - cellScaleSize).toInt(),
                                (eY - cellScaleSize).toInt()
                            )
                            mBitmapCell[index]!!.draw(canvas)
                        } else if (aCell.getmAnimationType() == PrimaryGame.MOVE_ANIMATION) // Moving animation
                        {
                            val percentDone = aCell.percentageDone
                            var tempIndex = index
                            if (aArray.size >= 2) tempIndex = tempIndex - 1
                            val previousX = aCell.mExtras[0]
                            val previousY = aCell.mExtras[1]
                            val currentX = currentTile.x
                            val currentY = currentTile.y
                            val dX =
                                ((currentX - previousX) * (mCellSize + mGridWidth) * (percentDone - 1) * 1.0).toInt()
                            val dY =
                                ((currentY - previousY) * (mCellSize + mGridWidth) * (percentDone - 1) * 1.0).toInt()
                            mBitmapCell[tempIndex]!!.setBounds(sX + dX, sY + dY, eX + dX, eY + dY)
                            mBitmapCell[tempIndex]!!.draw(canvas)
                        }
                        animated = true
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        mBitmapCell[index]!!.setBounds(sX, sY, eX, eY)
                        mBitmapCell[index]!!.draw(canvas)
                    }
                }
            }
        }
    }

    private fun drawEndGameState(canvas: Canvas) {
        var alphaChange = 1.0
        mContinueButtonEnabled = false
        for (animation in mGame.aGrid!!.mGlobalAnimation) if (animation.getmAnimationType() == PrimaryGame.FADE_GLOBAL_ANIMATION) alphaChange =
            animation.percentageDone
        var displayOverlay: BitmapDrawable? = null
        if (mGame.gameWon()) {
            if (mGame.canContinue()) {
                mContinueButtonEnabled = true
                displayOverlay = mWinGameContinueOverlay
            } else displayOverlay = mWinGameFinalOverlay
        } else if (mGame.gameLost()) displayOverlay = mLoseGameOverlay
        if (displayOverlay != null) {
            displayOverlay.setBounds(mStartingX, mStartingY, mEndingX, mEndingY)
            displayOverlay.alpha = (255 * alphaChange).toInt()
            displayOverlay.draw(canvas)
        }
    }

    private fun drawEndlessText(canvas: Canvas) {
        mPaint.textAlign = Paint.Align.LEFT
        mPaint.textSize = mBodyTextSize
        mPaint.color = resources.getColor(R.color.text_black)
        canvas.drawText(
            resources.getString(R.string.endless),
            mStartingX.toFloat(),
            (sYIcons - centerText() * 2).toFloat(),
            mPaint
        )
    }

    private fun drawGameOverButtons(canvas: Canvas) {
        drawNewGameButton(canvas, true)
        drawTrashButton(
            canvas,
            if (!mGame.gameWon() && PrimaryActivity.mRewardDeletes > 0) true else false
        )
        drawUndoButton(canvas, true)
        drawLoadButton(canvas, true)
        drawSaveButton(canvas, false)
    }

    private fun createEndGameStates(canvas: Canvas, win: Boolean, showButton: Boolean) {
        val width = mEndingX - mStartingX
        val length = mEndingY - mStartingY
        val middleX = width / 2
        val middleY = length / 2
        if (win) {
            mLightUpRectangle!!.alpha = 127
            drawDrawable(canvas, mLightUpRectangle, 0, 0, width, length)
            mLightUpRectangle!!.alpha = 255
            mPaint.color = resources.getColor(R.color.text_white)
            mPaint.alpha = 255
            mPaint.textSize = mGameOverTextSize
            mPaint.textAlign = Paint.Align.CENTER
            val textBottom = middleY - centerText()
            canvas.drawText(
                resources.getString(R.string.you_win),
                middleX.toFloat(),
                textBottom.toFloat(),
                mPaint
            )
            mPaint.textSize = mBodyTextSize
            val text =
                if (showButton) resources.getString(R.string.go_on) else resources.getString(R.string.for_now)
            canvas.drawText(
                text,
                middleX.toFloat(),
                (textBottom + mTextPaddingSize * 2 - centerText() * 2).toFloat(),
                mPaint
            )
        } else {
            mFadeRectangle!!.alpha = 127
            drawDrawable(canvas, mFadeRectangle, 0, 0, width, length)
            mFadeRectangle!!.alpha = 255
            mPaint.color = resources.getColor(R.color.text_black)
            mPaint.alpha = 255
            mPaint.textSize = mGameOverTextSize
            mPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                resources.getString(R.string.game_over),
                middleX.toFloat(),
                (middleY - centerText()).toFloat(),
                mPaint
            )
        }
    }

    private fun createBackgroundBitmap(width: Int, height: Int) {
        mBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mBackground!!)
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
        mPaint.textAlign = Paint.Align.CENTER
        for (xx in 1 until mBitmapCell.size) {
            val value = Math.pow(2.0, xx.toDouble()).toInt()
            mPaint.textSize = mCellTextSize
            val tempTextSize = mCellTextSize * mCellSize * 0.9f / Math.max(
                mCellSize * 0.9f,
                mPaint.measureText(value.toString())
            )
            mPaint.textSize = tempTextSize
            val bitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawDrawable(canvas, getDrawable(cellRectangleIds[xx]), 0, 0, mCellSize, mCellSize)
            drawCellText(canvas, value)
            mBitmapCell[xx] = BitmapDrawable(resources, bitmap)
        }
    }

    private val cellRectangleIds: IntArray
        private get() {
            val cellRectangleIds = IntArray(mNumCellTypes)
            cellRectangleIds[0] = R.drawable.gridcell_rectangle
            cellRectangleIds[1] = R.drawable.cell_rectangle_2
            cellRectangleIds[2] = R.drawable.cell_rectangle_4
            cellRectangleIds[3] = R.drawable.cell_rectangle_8
            cellRectangleIds[4] = R.drawable.gridcell_rectangle_16
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
            Bitmap.createBitmap(
                mEndingX - mStartingX,
                mEndingY - mStartingY,
                Bitmap.Config.ARGB_8888
            )
        var canvas = Canvas(bitmap!!)
        createEndGameStates(canvas, true, true)
        mWinGameContinueOverlay = BitmapDrawable(resources, bitmap)
        bitmap =
            Bitmap.createBitmap(
                mEndingX - mStartingX,
                mEndingY - mStartingY,
                Bitmap.Config.ARGB_8888
            )
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, true, false)
        mWinGameFinalOverlay = BitmapDrawable(resources, bitmap)
        bitmap =
            Bitmap.createBitmap(
                mEndingX - mStartingX,
                mEndingY - mStartingY,
                Bitmap.Config.ARGB_8888
            )
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, false, false)
        mLoseGameOverlay = BitmapDrawable(resources, bitmap)
    }

    private fun tick() {
        val currentTime = System.nanoTime()
        mGame.aGrid!!.tickAll(currentTime - mLastFPSTime)
        mLastFPSTime = currentTime
    }

    fun resyncTime() {
        mLastFPSTime = System.nanoTime()
    }

    private fun getLayout(width: Int, height: Int) {
        val ROWS = PrimaryMenuActivity.rows
        mCellSize = Math.min(width / (ROWS + 1), height / (ROWS + 3))
        mGridWidth = mCellSize / (ROWS + 3) // (ROWS + 3) was 7
        val screenMiddleX = width / 2
        val screenMiddleY = height / 2
        val boardMiddleY = screenMiddleY + mCellSize / 2
        mIconSize = mCellSize / 2

        //Grid Dimensions
        val halfNumSquaresX = ROWS / 2.0
        val halfNumSquaresY = ROWS / 2.0
        mStartingX =
            (screenMiddleX - (mCellSize + mGridWidth) * halfNumSquaresX - mGridWidth / 2).toInt()
        mEndingX =
            (screenMiddleX + (mCellSize + mGridWidth) * halfNumSquaresX + mGridWidth / 2).toInt()
        mStartingY =
            (boardMiddleY - (mCellSize + mGridWidth) * halfNumSquaresY - mGridWidth / 2).toInt()
        mEndingY =
            (boardMiddleY + (mCellSize + mGridWidth) * halfNumSquaresY + mGridWidth / 2).toInt()
        val widthWithPadding = (mEndingX - mStartingX).toFloat()

        // Text Dimensions
        mPaint.textSize = mCellSize.toFloat()
        mTextSize =
            mCellSize * mCellSize / Math.max(mCellSize.toFloat(), mPaint.measureText("0000"))
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.textSize = 1000f
        mGameOverTextSize = Math.min(
            Math.min(
                1000f * ((widthWithPadding - mGridWidth * 2) / mPaint.measureText(
                    resources.getString(
                        R.string.game_over
                    )
                )),
                mTextSize * 2
            ),
            1000f * ((widthWithPadding - mGridWidth * 2) / mPaint.measureText(resources.getString(R.string.you_win)))
        )
        mPaint.textSize = mCellSize.toFloat()
        mCellTextSize = mTextSize
        mTitleTextSize = mTextSize / 3
        mBodyTextSize = (mTextSize / 1.5).toFloat()
        mHeaderTextSize = mTextSize * 2
        mTextPaddingSize = (mTextSize / 3).toInt()
        mIconPaddingSize = (mTextSize / 5).toInt()
        mPaint.textSize = mTitleTextSize
        var textShiftYAll = centerText()
        //static variables
        sYAll = (mStartingY - mCellSize * 1.5).toInt()
        mTitleStartYAll = (sYAll + mTextPaddingSize + mTitleTextSize / 2 - textShiftYAll).toInt()
        mBodyStartYAll =
            (mTitleStartYAll + mTextPaddingSize + mTitleTextSize / 2 + mBodyTextSize / 2).toInt()
        mTitleWidthHighScore = mPaint.measureText(resources.getString(R.string.high_score)).toInt()
        mTitleWidthScore = mPaint.measureText(resources.getString(R.string.score)).toInt()
        mPaint.textSize = mBodyTextSize
        textShiftYAll = centerText()
        eYAll = (mBodyStartYAll + textShiftYAll + mBodyTextSize / 2 + mTextPaddingSize).toInt()
        sYIcons = (mStartingY + eYAll) / 2 - mIconSize / 2
        sXNewGame = mEndingX - mIconSize
        sXUndo = sXNewGame - mIconSize - mIconPaddingSize
        sXRemoveTiles = sXUndo - mIconSize - mIconPaddingSize
        sXLoad = sXRemoveTiles - mIconSize - mIconPaddingSize
        sXSave = sXLoad - mIconSize - mIconPaddingSize
        resyncTime()
    }

    private fun centerText(): Int {
        return ((mPaint.descent() + mPaint.ascent()) / 2).toInt()
    }

    companion object {
        //Internal Constants
        const val BASE_ANIMATION_TIME = 100000000
        private val TAG = PrimaryView::class.java.simpleName
        private const val MERGING_ACCELERATION = (-0.5).toFloat()
        private const val INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4
        private fun log2(n: Int): Int {
            require(n > 0)
            return 31 - Integer.numberOfLeadingZeros(n)
        }
    }
}