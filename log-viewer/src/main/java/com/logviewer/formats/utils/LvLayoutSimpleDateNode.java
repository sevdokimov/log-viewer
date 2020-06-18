package com.logviewer.formats.utils;

import com.logviewer.utils.LvDateUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LvLayoutSimpleDateNode extends LvLayoutDateNode {

    private final String format;

    private transient SimpleDateFormat dateFormat;

    private transient ParsePosition parsePosition;

    public LvLayoutSimpleDateNode(String format) {
        this.format = format;
        dateFormat = new SimpleDateFormat(format);
        parsePosition = new ParsePosition(0);
    }

    public String getFormat() {
        return format;
    }

    @Override
    public int parse(String s, int offset, int end) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(format);
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
    public LvLayoutNode clone() {
        return new LvLayoutSimpleDateNode(format);
    }
}
