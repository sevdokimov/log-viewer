package com.logviewer.filters;

import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Record;

import java.util.List;

public class FieldValueSetPredicate implements RecordPredicate {

    private String fieldType;

    private List<String> values;

    public FieldValueSetPredicate() {

    }

    public FieldValueSetPredicate(String fieldType, List<String> values) {
        this.fieldType = fieldType;
        this.values = values;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        if (fieldType == null || values == null)
            return false;

        LogFormat.FieldDescriptor[] fields = ctx.getFields();
        for (int i = 0; i < fields.length; i++) {
            LogFormat.FieldDescriptor field = fields[i];
            if (FieldTypes.is(field.type(), fieldType)) {
                String fieldValue = record.getFieldText(i);

                if (fieldValue != null && values.contains(fieldValue))
                    return true;
            }
        }

        return false;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
