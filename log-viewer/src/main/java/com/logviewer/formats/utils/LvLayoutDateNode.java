package com.logviewer.formats.utils;

public abstract class LvLayoutDateNode implements LvLayoutNode {

    protected transient long currentDate;

    public long getCurrentDate() {
        return currentDate;
    }

    @Override
    public boolean removeSpacesBefore() {
        return false;
    }

    public abstract boolean isFull();

    @Override
    public abstract LvLayoutNode clone();
}
