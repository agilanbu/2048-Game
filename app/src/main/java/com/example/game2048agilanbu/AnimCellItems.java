package com.example.game2048agilanbu;

class AnimCellItems extends CellItem
{
    public final int[] mExtras;
    private final int animationType;
    private final long animationTime;
    private final long delayTime;
    private long timeElapsed;

    public AnimCellItems(int x, int y, int animationType, long length, long delay, int[] extras)
    {
        super(x, y);
        this.animationType = animationType;
        animationTime = length;
        delayTime = delay;
        this.mExtras = extras;
    }

    public int getAnimationType()
    {
        return animationType;
    }

    public void tick(long timeElapsed)
    {
        this.timeElapsed = this.timeElapsed + timeElapsed;
    }

    public boolean animationDone()
    {
        return animationTime + delayTime < timeElapsed;
    }

    public double getPercentageDone()
    {
        return Math.max(0, 1.0 * (timeElapsed - delayTime) / animationTime);
    }

    public boolean isActive()
    {
        return (timeElapsed >= delayTime);
    }
}