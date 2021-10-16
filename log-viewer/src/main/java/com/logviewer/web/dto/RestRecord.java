package com.logviewer.web.dto;

import com.google.gson.annotations.JsonAdapter;
import com.logviewer.data2.LogRecord;
import com.logviewer.utils.GsonNanosecondsAdapter;
import com.logviewer.utils.Pair;
import com.logviewer.utils.TextRange;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RestRecord {

    private final String logId;

    private final String s;
    private final long start;
    private final long end;

    private final boolean hasMore;

    @JsonAdapter(GsonNanosecondsAdapter.class)
    private final Long time;

    private final List<RestField> fields;

    private final String filteringError;

    public RestRecord(@NonNull LogRecord record) {
        this(record, null);
    }

    public RestRecord(@NonNull Pair<LogRecord, Throwable> pair) {
        this(pair.getFirst(), pair.getSecond() == null ? null : Utils.getStackTraceAsString(pair.getSecond()));
    }

    public RestRecord(@NonNull LogRecord record, @Nullable String filteringError) {
        this.logId = record.getLogId();

        s = record.getMessage();
        start = record.getStart();
        end = record.getEnd();
        hasMore = record.hasMore();

        fields = new ArrayList<>();;
        for (String fieldName : record.getFieldNames()) {
            TextRange fieldOffset = record.getFieldOffset(fieldName);
            if (fieldOffset != null)
                this.fields.add(new RestField(fieldName, fieldOffset.getStart(), fieldOffset.getEnd()));
        }

        this.filteringError = filteringError;

        time = record.getTime();
    }

    public String getLogId() {
        return logId;
    }

    public String fieldValue(@NonNull String fieldName) {
        return fields.stream()
                .filter(f -> f.name.equals(fieldName))
                .findFirst()
                .map(f -> s.substring(f.start, f.end))
                .orElse(null);
    }

    public String getText() {
        return s;
    }

    @Override
    public String toString() {
        return s;
    }

    public static List<RestRecord> fromPairList(@Nullable List<Pair<LogRecord, Throwable>> pairs) {
        if (pairs == null)
            return null;

        List<RestRecord> res = new ArrayList<>(pairs.size());

        for (Pair<LogRecord, Throwable> pair : pairs) {
            res.add(new RestRecord(pair));
        }

        return res;
    }

    private static class RestField {
        String name;
        int start;
        int end;

        public RestField(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }
}
