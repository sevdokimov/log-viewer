package com.logviewer.data2;

import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class LvPredicateChecker implements LogFilterContext {

    private final LogView logView;
    private final LogFormat logFormat;
    private final String logId;

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
    public String getFieldValue(@NonNull Record record, @NonNull String fieldName) {
        assert logId.equals(record.getLogId());

        int fieldIndex = findFieldIndexByName(fieldName);
        return fieldIndex < 0 ? null : record.getFieldText(fieldIndex);
    }

    @NonNull
    @Override
    public LogFormat.FieldDescriptor[] getFields() {
        return logFormat.getFields();
    }

    public Pair<Record, Throwable> applyFilter(@NonNull Record record, @Nullable RecordPredicate filter) {
        try {
            if (filter != null && !filter.test(record, this))
                return null;
        } catch (Throwable e) {
            return new Pair<>(record, e);
        }

        return new Pair<>(record, null);
    }
}
