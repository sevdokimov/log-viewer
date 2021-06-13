package com.logviewer.formats.utils;

import com.logviewer.utils.LvDateUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class LvLayoutSimpleDateNode extends LvLayoutDateNode {

    private final String format;

    private transient BiFunction<String, ParsePosition, Supplier<Instant>> formatter;

    protected transient Supplier<Instant> timestamp;

    public LvLayoutSimpleDateNode(@NonNull String format) {
        this(format, null);
    }

    public LvLayoutSimpleDateNode(@NonNull String format, @Nullable TimeZone zone) {
        super(zone);
        this.format = format;
        FastDateTimeParser.createFormatter(format, null); // validation
    }

    public String getFormat() {
        return format;
    }

    @Override
    public int parse(String s, int offset, int end) {
        ParsePosition position = new ParsePosition(offset);

        if (formatter == null)
            formatter = FastDateTimeParser.createFormatter(format, zone);

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
        return LvDateUtils.isDateFormatFull(new SimpleDateFormat(format));
    }

    @Override
    public LvLayoutDateNode clone() {
        return new LvLayoutSimpleDateNode(format, zone);
    }
}
