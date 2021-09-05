package com.agilanbu.game2048kotlin

import android.app.AlertDialog
import android.view.View.OnTouchListener
import android.view.MotionEvent
import android.view.View

internal class GestureInputListener(private val mView: PrimaryView) : OnTouchListener {
    private var x = 0f
    private var y = 0f
    private var mLastDx = 0f
    private var mLastDy = 0f
    private var mPreviousX = 0f
    private var mPreviousY = 0f
    private var mStartingX = 0f
    private var mStartingY = 0f
    private var mPreviousDirection = 1
    private var mVeryLastDirection = 1
    private var mHasMoved = false
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                x = event.x
                y = event.y
                mStartingX = x
                mStartingY = y
                mPreviousX = x
                mPreviousY = y
                mLastDx = 0f
                mLastDy = 0f
                mHasMoved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                x = event.x
                y = event.y
                if (mView.mGame.isActive) {
                    val dx = x - mPreviousX
                    if (Math.abs(mLastDx + dx) < Math.abs(mLastDx) + Math.abs(dx) && Math.abs(dx) > RESET_STARTING && Math.abs(
                            x - mStartingX
                        ) > SWIPE_MIN_DISTANCE
                    ) {
                        mStartingX = x
                        mStartingY = y
                        mLastDx = dx
                        mPreviousDirection = mVeryLastDirection
                    }
                    if (mLastDx == 0f) {
                        mLastDx = dx
                    }
                    val dy = y - mPreviousY
                    if (Math.abs(mLastDy + dy) < Math.abs(mLastDy) + Math.abs(dy) && Math.abs(dy) > RESET_STARTING && Math.abs(
                            y - mStartingY
                        ) > SWIPE_MIN_DISTANCE
                    ) {
                        mStartingX = x
                        mStartingY = y
                        mLastDy = dy
                        mPreviousDirection = mVeryLastDirection
                    }
                    if (mLastDy == 0f) {
                        mLastDy = dy
                    }
                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE && !mHasMoved) {
                        var moved = false
                        //Vertical
                        if ((dy >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx) || y - mStartingY >= MOVE_THRESHOLD) && mPreviousDirection % 2 != 0) {
                            moved = true
                            mPreviousDirection = mPreviousDirection * 2
                            mVeryLastDirection = 2
                            mView.mGame.move(2)
                        } else if ((dy <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx) || y - mStartingY <= -MOVE_THRESHOLD) && mPreviousDirection % 3 != 0) {
                            moved = true
                            mPreviousDirection = mPreviousDirection * 3
                            mVeryLastDirection = 3
                            mView.mGame.move(0)
                        }
                        //Horizontal
                        if ((dx >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy) || x - mStartingX >= MOVE_THRESHOLD) && mPreviousDirection % 5 != 0) {
                            moved = true
                            mPreviousDirection = mPreviousDirection * 5
                            mVeryLastDirection = 5
                            mView.mGame.move(1)
                        } else if ((dx <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy) || x - mStartingX <= -MOVE_THRESHOLD) && mPreviousDirection % 7 != 0) {
                            moved = true
                            mPreviousDirection = mPreviousDirection * 7
                            mVeryLastDirection = 7
                            mView.mGame.move(3)
                        }
                        if (moved) {
                            mHasMoved = true
                            mStartingX = x
                            mStartingY = y
                        }
                    }
                }
                mPreviousX = x
                mPreviousY = y
                return true
            }
            MotionEvent.ACTION_UP -> {
                x = event.x
                y = event.y
                mPreviousDirection = 1
                mVeryLastDirection = 1

                //"Menu" inputs
                if (!mHasMoved) {
                    if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                        AlertDialog.Builder(mView.context)
                            .setPositiveButton(R.string.reset) { dialog, which -> // reset rewards again:
                                PrimaryActivity.mRewardDeletes = 2
                                PrimaryActivity.mRewardDeletingSelectionAmounts = 3
                                mView.mGame.newGame()
                                mView.mGame.mCanUndo = false
                            }
                            .setNegativeButton(R.string.continue_game, null)
                            .setTitle(R.string.reset_dialog_title)
                            .setMessage(R.string.reset_dialog_message)
                            .show()
                    } else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
                        mView.mGame.revertUndoState()
                    } else if (iconPressed(mView.sXRemoveTiles, mView.sYIcons)) {
                        mView.mGame.RemoveTilesWithTrash()
                    } else if (iconPressed(mView.sXLoad, mView.sYIcons)) {
                        mView.mGame.loadCurrentBoard() // load previous board
                    } else if (iconPressed(mView.sXSave, mView.sYIcons)) {
                        mView.mGame.saveCurrentBoard() // save current board
                    } else if (isTap(2) && inRange(
                            mView.mStartingX.toFloat(),
                            x,
                            mView.mEndingX.toFloat()
                        )
                        && inRange(
                            mView.mStartingY.toFloat(),
                            x,
                            mView.mEndingY.toFloat()
                        ) && mView.mContinueButtonEnabled
                    ) {
                        mView.mGame.setEndlessMode()
                    }
                }
            }
        }
        return true
    }

    private fun pathMoved(): Float {
        return (x - mStartingX) * (x - mStartingX) + (y - mStartingY) * (y - mStartingY)
    }

    private fun iconPressed(sx: Int, sy: Int): Boolean {
        return (isTap(1) && inRange(sx.toFloat(), x, (sx + mView.mIconSize).toFloat())
                && inRange(sy.toFloat(), y, (sy + mView.mIconSize).toFloat()))
    }

    private fun inRange(starting: Float, check: Float, ending: Float): Boolean {
        return starting <= check && check <= ending
    }

    private fun isTap(factor: Int): Boolean {
        return pathMoved() <= mView.mIconSize * factor
    }

    companion object {
        private const val SWIPE_MIN_DISTANCE = 0
        private const val SWIPE_THRESHOLD_VELOCITY = 25
        private const val MOVE_THRESHOLD = 250
        private const val RESET_STARTING = 10
    }
}