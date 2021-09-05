package com.agilanbu.game2048kotlin;

import java.util.ArrayList;

public class AnimGridItems {
    public final ArrayList<AnimCellItems> mGlobalAnimation = new ArrayList<>();
    private final ArrayList<AnimCellItems>[][] mField;
    private int mActiveAnimations = 0;
    private boolean mOneMoreFrame = false;

    @SuppressWarnings("unchecked")
    public AnimGridItems(int x, int y) {
        mField = new ArrayList[x][y];

        for (int xx = 0; xx < x; xx++) {
            for (int yy = 0; yy < y; yy++) {
                mField[xx][yy] = new ArrayList<>();
            }
        }
    }

    public void startAnimation(int x, int y, int animationType, long length, long delay, int[] extras) {
        AnimCellItems animationToAdd = new AnimCellItems(x, y, animationType, length, delay, extras);
        if (x == -1 && y == -1) {
            mGlobalAnimation.add(animationToAdd);
        } else {
            mField[x][y].add(animationToAdd);
        }
        mActiveAnimations = mActiveAnimations + 1;
    }

    public void tickAll(long timeElapsed) {
        ArrayList<AnimCellItems> cancelledAnimations = new ArrayList<>();
        for (AnimCellItems animation : mGlobalAnimation) {
            animation.tick(timeElapsed);
            if (animation.animationDone()) {
                cancelledAnimations.add(animation);
                mActiveAnimations = mActiveAnimations - 1;
            }
        }

        for (ArrayList<AnimCellItems>[] array : mField) {
            for (ArrayList<AnimCellItems> list : array) {
                for (AnimCellItems animation : list) {
                    animation.tick(timeElapsed);
                    if (animation.animationDone()) {
                        cancelledAnimations.add(animation);
                        mActiveAnimations = mActiveAnimations - 1;
                    }
                }
            }
        }

        for (AnimCellItems animation : cancelledAnimations) {
            cancelAnimation(animation);
        }
    }

    public boolean isAnimationActive() {
        if (mActiveAnimations != 0) {
            mOneMoreFrame = true;
            return true;
        } else if (mOneMoreFrame) {
            mOneMoreFrame = false;
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<AnimCellItems> getAnimationCell(int x, int y) {
        return mField[x][y];
    }

    public void cancelAnimations() {
        for (ArrayList<AnimCellItems>[] array : mField) {
            for (ArrayList<AnimCellItems> list : array) {
                list.clear();
            }
        }
        mGlobalAnimation.clear();
        mActiveAnimations = 0;
    }

    private void cancelAnimation(AnimCellItems animation) {
        if (animation.getX() == -1 && animation.getY() == -1) {
            mGlobalAnimation.remove(animation);
        } else {
            mField[animation.getX()][animation.getY()].remove(animation);
        }
    }

}
