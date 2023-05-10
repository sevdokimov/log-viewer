package com.logviewer.formats.utils;

import com.logviewer.utils.LvDateUtils;
import org.springframework.lang.NonNull;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class LvLayoutSimpleDateNode extends LvLayoutDateNode {

    private final String format;

    private transient BiFunction<String, ParsePosition, Supplier<Instant>> formatter;

    protected transient Supplier<Instant> timestamp;

    public LvLayoutSimpleDateNode(@NonNull String format) {
        this.format = format;
        FastDateTimeParser.createFormatter(format, null, null); // validation
    }

    public String getFormat() {
        return format;
    }

    @Override
    public int parse(String s, int offset, int end) {
        ParsePosition position = new ParsePosition(offset);

        if (formatter == null)
            formatter = FastDateTimeParser.createFormatter(format, locale, zone);

        timestamp = formatter.apply(s, position);
        if (timestamp == null || position.getIndex() > end) {
            currentDate = -1;
            return PARSE_FAILED;
        }

        return position.getIndex();
    }

    @Override
    public long getCurrentDate() {
        Instant instant = timestamp.get();

        return LvDateUtils.toNanos(instant);
    }

    @Override
    public boolean isFull() {
        SimpleDateFormat sompleFormat = locale != null ? new SimpleDateFormat(format, locale) : new SimpleDateFormat(format);
        
        return LvDateUtils.isDateFormatFull(sompleFormat);
    }

    @Override
    public LvLayoutDateNode clone() {
        LvLayoutSimpleDateNode res = (LvLayoutSimpleDateNode) super.clone();

        // Clear transient fields
        res.timestamp = null;
        res.formatter = null;

        return res;
    }
}
