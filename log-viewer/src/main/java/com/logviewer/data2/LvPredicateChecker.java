package com.logviewer.data2;

import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LvPredicateChecker implements LogFilterContext, AutoCloseable {

    private final LogView logView;
    private final LogFormat logFormat;
    private final String logId;

    private Map<String, Object> context;

    public LvPredicateChecker(LogView logView) {
        this.logView = logView;
        this.logFormat = logView.getFormat();
        this.logId = logView.getId();
    }

    @Override
    public LogFormat getLogFormat() {
        return logFormat;
    }

    @Override
    public int findFieldIndexByName(@NonNull String fieldName) {
        return logFormat.getFieldIndexByName(fieldName);
    }

    @Nullable
    @Override
    public String getFieldValue(@NonNull LogRecord record, @NonNull String fieldName) {
        assert logId.equals(record.getLogId());

        int fieldIndex = findFieldIndexByName(fieldName);
        return fieldIndex < 0 ? null : record.getFieldText(fieldIndex);
    }

    @NonNull
    @Override
    public LogFormat.FieldDescriptor[] getFields() {
        return logFormat.getFields();
    }

    @NonNull
    @Override
    public <T> T getProperty(String name, Function<String, T> factory) {
        if (context == null) {
            context = new HashMap<>();
        }

        return (T)context.computeIfAbsent(name, factory);
    }

    public Pair<LogRecord, Throwable> applyFilter(@NonNull LogRecord record, @Nullable RecordPredicate filter) {
        try {
            if (filter != null && !filter.test(record, this))
                return null;
        } catch (Throwable e) {
            return new Pair<>(record, e);
        }

        return new Pair<>(record, null);
    }

    @Override
    public void close() throws Exception {
        if (context != null) {
            for (Object value : context.values()) {
                if (value instanceof AutoCloseable) {
                    Utils.closeQuietly((AutoCloseable) value);
                }
            }
        }
    }
}
