package com.agilanbu.game2048kotlin;

class AnimCellItems extends CellItem
{
    public final int[] mExtras;
    private final int mAnimationType;
    private final long mAnimationTime;
    private final long mDelayTime;
    private long mTimeElapsed;

    public AnimCellItems(int x, int y, int animationType, long length, long delay, int[] extras)
    {
        super(x, y);
        this.mAnimationType = animationType;
        mAnimationTime = length;
        mDelayTime = delay;
        this.mExtras = extras;
    }

    public int getmAnimationType()
    {
        return mAnimationType;
    }

    public void tick(long timeElapsed)
    {
        this.mTimeElapsed = this.mTimeElapsed + timeElapsed;
    }

    public boolean animationDone()
    {
        return mAnimationTime + mDelayTime < mTimeElapsed;
    }

    public double getPercentageDone()
    {
        return Math.max(0, 1.0 * (mTimeElapsed - mDelayTime) / mAnimationTime);
    }

    public boolean isActive()
    {
        return (mTimeElapsed >= mDelayTime);
    }
}