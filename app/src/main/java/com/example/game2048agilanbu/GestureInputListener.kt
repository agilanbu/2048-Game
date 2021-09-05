package com.example.game2048agilanbu

import android.app.AlertDialog
import android.view.View.OnTouchListener
import android.view.MotionEvent
import android.view.View

internal class GestureInputListener(private val mView: MainView) : OnTouchListener {
    private var x = 0f
    private var y = 0f
    private var lastDx = 0f
    private var lastDy = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var startingX = 0f
    private var startingY = 0f
    private var previousDirection = 1
    private var veryLastDirection = 1
    private var hasMoved = false
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                x = event.x
                y = event.y
                startingX = x
                startingY = y
                previousX = x
                previousY = y
                lastDx = 0f
                lastDy = 0f
                hasMoved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                x = event.x
                y = event.y
                if (mView.game.isActive) {
                    val dx = x - previousX
                    if (Math.abs(lastDx + dx) < Math.abs(lastDx) + Math.abs(dx) && Math.abs(dx) > RESET_STARTING && Math.abs(
                            x - startingX
                        ) > SWIPE_MIN_DISTANCE
                    ) {
                        startingX = x
                        startingY = y
                        lastDx = dx
                        previousDirection = veryLastDirection
                    }
                    if (lastDx == 0f) {
                        lastDx = dx
                    }
                    val dy = y - previousY
                    if (Math.abs(lastDy + dy) < Math.abs(lastDy) + Math.abs(dy) && Math.abs(dy) > RESET_STARTING && Math.abs(
                            y - startingY
                        ) > SWIPE_MIN_DISTANCE
                    ) {
                        startingX = x
                        startingY = y
                        lastDy = dy
                        previousDirection = veryLastDirection
                    }
                    if (lastDy == 0f) {
                        lastDy = dy
                    }
                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE && !hasMoved) {
                        var moved = false
                        //Vertical
                        if ((dy >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx) || y - startingY >= MOVE_THRESHOLD) && previousDirection % 2 != 0) {
                            moved = true
                            previousDirection = previousDirection * 2
                            veryLastDirection = 2
                            mView.game.move(2)
                        } else if ((dy <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx) || y - startingY <= -MOVE_THRESHOLD) && previousDirection % 3 != 0) {
                            moved = true
                            previousDirection = previousDirection * 3
                            veryLastDirection = 3
                            mView.game.move(0)
                        }
                        //Horizontal
                        if ((dx >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy) || x - startingX >= MOVE_THRESHOLD) && previousDirection % 5 != 0) {
                            moved = true
                            previousDirection = previousDirection * 5
                            veryLastDirection = 5
                            mView.game.move(1)
                        } else if ((dx <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy) || x - startingX <= -MOVE_THRESHOLD) && previousDirection % 7 != 0) {
                            moved = true
                            previousDirection = previousDirection * 7
                            veryLastDirection = 7
                            mView.game.move(3)
                        }
                        if (moved) {
                            hasMoved = true
                            startingX = x
                            startingY = y
                        }
                    }
                }
                previousX = x
                previousY = y
                return true
            }
            MotionEvent.ACTION_UP -> {
                x = event.x
                y = event.y
                previousDirection = 1
                veryLastDirection = 1

                //"Menu" inputs
                if (!hasMoved) {
                    if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                        AlertDialog.Builder(mView.context)
                            .setPositiveButton(R.string.reset) { dialog, which -> // reset rewards again:
                                PrimaryActivity.mRewardDeletes = 2
                                PrimaryActivity.mRewardDeletingSelectionAmounts = 3
                                mView.game.newGame()
                                mView.game.canUndo = false
                            }
                            .setNegativeButton(R.string.continue_game, null)
                            .setTitle(R.string.reset_dialog_title)
                            .setMessage(R.string.reset_dialog_message)
                            .show()
                    } else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
                        mView.game.revertUndoState()
                    } else if (iconPressed(mView.sXRemoveTiles, mView.sYIcons)) {
                        mView.game.RemoveTilesWithTrash()
                    } else if (iconPressed(mView.sXLoad, mView.sYIcons)) {
                        mView.game.loadCurrentBoard() // load previous board
                    } else if (iconPressed(mView.sXSave, mView.sYIcons)) {
                        mView.game.saveCurrentBoard() // save current board
                    } else if (isTap(2) && inRange(
                            mView.startingX.toFloat(),
                            x,
                            mView.endingX.toFloat()
                        )
                        && inRange(
                            mView.startingY.toFloat(),
                            x,
                            mView.endingY.toFloat()
                        ) && mView.continueButtonEnabled
                    ) {
                        mView.game.setEndlessMode()
                    }
                }
            }
        }
        return true
    }

    private fun pathMoved(): Float {
        return (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY)
    }

    private fun iconPressed(sx: Int, sy: Int): Boolean {
        return (isTap(1) && inRange(sx.toFloat(), x, (sx + mView.iconSize).toFloat())
                && inRange(sy.toFloat(), y, (sy + mView.iconSize).toFloat()))
    }

    private fun inRange(starting: Float, check: Float, ending: Float): Boolean {
        return starting <= check && check <= ending
    }

    private fun isTap(factor: Int): Boolean {
        return pathMoved() <= mView.iconSize * factor
    }

    companion object {
        private const val SWIPE_MIN_DISTANCE = 0
        private const val SWIPE_THRESHOLD_VELOCITY = 25
        private const val MOVE_THRESHOLD = 250
        private const val RESET_STARTING = 10
    }
}