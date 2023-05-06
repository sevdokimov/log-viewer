package com.logviewer.formats.utils;

import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.TimeZone;

public abstract class LvLayoutDateNode implements LvLayoutNode {

    protected transient long currentDate;

    protected TimeZone zone;

    protected Locale locale;

    public TimeZone getZone() {
        return zone;
    }

    public long getCurrentDate() {
        return currentDate;
    }

    public LvLayoutDateNode withTimeZone(@Nullable TimeZone zone) {
        if (zone == this.zone)
            return this;

        LvLayoutDateNode res = clone();
        res.zone = zone;
        return res;
    }

    public LvLayoutDateNode withLocale(@Nullable Locale locale) {
        if (locale == this.locale)
            return this;

        LvLayoutDateNode res = clone();
        res.locale = locale;
        return res;
    }

    public abstract boolean isFull();

    @Override
    public LvLayoutDateNode clone() {
        try {
            return (LvLayoutDateNode) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
