package com.logviewer.formats.utils;

import org.springframework.lang.Nullable;

import java.util.TimeZone;

public abstract class LvLayoutDateNode implements LvLayoutNode {

    protected transient long currentDate;

    protected TimeZone zone;

    protected LvLayoutDateNode(@Nullable TimeZone zone) {
        this.zone = zone;
    }

    public TimeZone getZone() {
        return zone;
    }

    public long getCurrentDate() {
        return currentDate;
    }

    public LvLayoutDateNode withTimeZone(@Nullable TimeZone zone) {
        LvLayoutDateNode res = clone();
        res.zone = zone;
        return res;
    }

    public abstract boolean isFull();

    @Override
    public abstract LvLayoutDateNode clone();
}
