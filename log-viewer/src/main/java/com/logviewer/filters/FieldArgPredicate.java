package com.logviewer.filters;

import com.logviewer.data2.LogFilterContext;
import com.logviewer.data2.Record;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 *
 */
public class FieldArgPredicate implements RecordPredicate {

    private String fieldName;

    private String value;

    private Operator operator = Operator.EQUALS;

    public FieldArgPredicate(@Nonnull String fieldName, String value) {
        this(fieldName, value, Operator.EQUALS);
    }

    public FieldArgPredicate(@Nonnull String fieldName, String value, @Nonnull Operator operator) {
        this.fieldName = fieldName;
        this.value = value;
        this.operator = operator;
    }

    @Nonnull
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(@Nonnull String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Nonnull
    public Operator getOperator() {
        return operator;
    }

    public void setOperator(@Nonnull Operator operator) {
        this.operator = operator;
    }

    @Override
    public boolean test(Record record, LogFilterContext ctx) {
        String fieldValue;

        if (Record.WHOLE_LINE.equals(fieldName)) {
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
