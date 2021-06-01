package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.LogRecord;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 *
 */
public class FieldArgPredicate implements RecordPredicate {

    private String fieldName;

    private String value;

    private Operator operator = Operator.EQUALS;

    public FieldArgPredicate(@NonNull String fieldName, String value) {
        this(fieldName, value, Operator.EQUALS);
    }

    public FieldArgPredicate(@NonNull String fieldName, String value, @NonNull Operator operator) {
        this.fieldName = fieldName;
        this.value = value;
        this.operator = operator;
    }

    @NonNull
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(@NonNull String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @NonNull
    public Operator getOperator() {
        return operator;
    }

    public void setOperator(@NonNull Operator operator) {
        this.operator = operator;
    }

    @Override
    public boolean test(LogRecord record, LogFilterContext ctx) {
        String fieldValue;

        if (LogRecord.WHOLE_LINE.equals(fieldName)) {
            fieldValue = record.getMessage();
        } else {
            fieldValue = ctx.getFieldValue(record, fieldName);
        }

        return operator.test(fieldValue, value);
    }

    public enum Operator {
        EQUALS {
            @Override
            public boolean test(String fieldValue, String arg) {
                return Objects.equals(fieldValue, arg);
            }
        },

        NOT_EQUALS {
            @Override
            public boolean test(String fieldValue, String arg) {
                return !Objects.equals(fieldValue, arg);
            }
        },

        IEQUALS {
            @Override
            public boolean test(String fieldValue, String arg) {
                return fieldValue == null ? arg == null :
                        fieldValue.equalsIgnoreCase(arg);
            }
        },

        NOT_IEQUALS {
            @Override
            public boolean test(String fieldValue, String arg) {
                return !(fieldValue == null ? arg == null :
                        fieldValue.equalsIgnoreCase(arg));
            }
        },

        CONTAINS {
            @Override
            public boolean test(String fieldValue, String arg) {
                return fieldValue != null && arg != null && fieldValue.contains(arg);
            }
        },

        START_WITH {
            @Override
            public boolean test(String fieldValue, String arg) {
                return fieldValue != null && arg != null && fieldValue.startsWith(arg);
            }
        },

        END_WITH {
            @Override
            public boolean test(String fieldValue, String arg) {
                return fieldValue != null && arg != null && fieldValue.endsWith(arg);
            }
        },

        ;

        public abstract boolean test(String fieldValue, String arg);

    }
}
