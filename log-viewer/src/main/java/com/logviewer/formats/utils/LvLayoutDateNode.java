package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.TimeZone;

public abstract class LvLayoutDateNode implements LvLayoutNode {

    protected transient long currentDate;

    protected TimeZone zone;

    protected Locale locale;

    protected LvLayoutDateNode(@Nullable Locale locale, @Nullable TimeZone zone) {
        this.locale = locale;
        this.zone = zone;
    }

    public Locale getLocale() { return locale; }

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
