package com.logviewer.web.dto;

import com.google.common.base.Throwables;
import com.logviewer.data2.Record;
import com.logviewer.utils.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RestRecord {

    private final String logId;

    private final String s;
    private final long start;
    private final long end;

    private final boolean hasMore;

    private final long time;

    private final int[] fieldsOffsetStart;
    private final int[] fieldsOffsetEnd;

    private final String filteringError;

    public RestRecord(@Nonnull Record record) {
        this(record, null);
    }

    public RestRecord(@Nonnull Pair<Record, Throwable> pair) {
        this(pair.getFirst(), pair.getSecond() == null ? null : Throwables.getStackTraceAsString(pair.getSecond()));
    }

    public RestRecord(@Nonnull Record record, @Nullable String filteringError) {
        this.logId = record.getLogId();

        s = record.getMessage();
        start = record.getStart();
        end = record.getEnd();
        hasMore = record.hasMore();

        fieldsOffsetStart = new int[record.getFieldsCount()];
        fieldsOffsetEnd = new int[record.getFieldsCount()];

        for (int i = 0; i < fieldsOffsetStart.length; i++) {
            fieldsOffsetStart[i] = record.getFieldStart(i);
            fieldsOffsetEnd[i] = record.getFieldEnd(i);
        }

        this.filteringError = filteringError;

        time = record.getTime();
    }

    public String getLogId() {
        return logId;
    }

    public int[] getFieldsOffsetStart() {
        return fieldsOffsetStart;
    }

    public int[] getFieldsOffsetEnd() {
        return fieldsOffsetEnd;
    }

    public String fieldValue(int fieldIndex) {
        if (fieldIndex < 0)
            return null;
        
        if (fieldsOffsetStart[fieldIndex] == -1)
            return null;
        
        return s.substring(fieldsOffsetStart[fieldIndex], fieldsOffsetEnd[fieldIndex]);
    }

    public String getText() {
        return s;
    }

    @Override
    public String toString() {
        return s;
    }

    public static List<RestRecord> fromPairList(@Nullable List<Pair<Record, Throwable>> pairs) {
        if (pairs == null)
            return null;

        List<RestRecord> res = new ArrayList<>(pairs.size());

        Record prev = null;
        for (Pair<Record, Throwable> pair : pairs) {
            assert prev == null || prev.compareTo(pair.getFirst()) < 0;
            prev = pair.getFirst();

            res.add(new RestRecord(pair));
        }

        return res;
    }
}
