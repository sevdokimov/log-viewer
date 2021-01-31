package com.logviewer.formats.utils;

import com.logviewer.utils.LvDateUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LvLayoutSimpleDateNode extends LvLayoutDateNode {

    private final String format;

    private transient SimpleDateFormat dateFormat;

    private transient ParsePosition parsePosition;

    public LvLayoutSimpleDateNode(@NonNull String format) {
        this(format, null);
    }

    public LvLayoutSimpleDateNode(@NonNull String format, @Nullable TimeZone zone) {
        super(zone);
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public int parse(String s, int offset, int end) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(format);
            if (zone != null)
                dateFormat.setTimeZone(zone);

            parsePosition = new ParsePosition(0);
        }

        parsePosition.setIndex(offset);

        Date date = dateFormat.parse(s, parsePosition);
        if (date == null) {
            currentDate = -1;
            return PARSE_FAILED;
        }

        currentDate = date.getTime();

        return parsePosition.getIndex();
    }

    @Override
    public boolean isFull() {
        return LvDateUtils.isDateFormatFull(new SimpleDateFormat(format));
    }

    @Override
    public LvLayoutDateNode clone() {
        return new LvLayoutSimpleDateNode(format, zone);
    }
}
