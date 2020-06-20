package com.logviewer.formats;

import com.logviewer.data2.DefaultFieldDesciptor;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import com.logviewer.data2.Record;
import com.logviewer.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexLogFormat implements LogFormat, Cloneable {

    private Charset charset;

    private String regexp;

    private RegexpField[] fields;

    private int fieldToExtend = -1;

    private Integer dateFieldIdx;
    private String datePattern;

    private transient volatile Pattern pattern;

    public RegexLogFormat(@Nonnull Charset charset, @Nullable String fieldToExtend,
                          @Nonnull String regexp, RegexpField ... fields) {
        this(charset, fieldToExtend, regexp, null, null, fields);
    }

    public RegexLogFormat(@Nullable Charset charset, @Nullable String fieldToExtend,
                          @Nonnull String regexp,
                          @Nullable String datePattern, @Nullable String dateField,
                          RegexpField ... fields) {
        this.regexp = regexp;
        this.charset = charset;

        this.fields = fields;

        if (fieldToExtend != null) {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].name().equals(fieldToExtend)) {
                    this.fieldToExtend = i;
                    break;
                }
            }
            
            if (this.fieldToExtend < 0)
                throw new IllegalArgumentException("Field not found: " + fieldToExtend);
        }

        this.datePattern = datePattern;

        if (dateField != null) {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].name().equals(dateField)) {
                    this.dateFieldIdx = i;
                    break;
                }
            }

            if (this.dateFieldIdx == null)
                throw new IllegalArgumentException("Field not found: " + dateField);
        }

        validate();
    }

    private Pattern getPattern() {
        Pattern res = pattern;

        if (res == null) {
            String regexp = this.regexp;

            res = Pattern.compile(regexp);

            pattern = res;
        }

        return res;
    }

    private SimpleDateFormat createDateFormatter() {
        return new SimpleDateFormat(datePattern);
    }

    private void validate() {
        int groupCount = getPattern().matcher("").groupCount();

        Set<Object> usedGroups = new HashSet<>();

        for (RegexpField field : fields) {
            if (field.name() == null || field.name().isEmpty())
                throw new IllegalArgumentException("Filed name can not be empty string");

            if (!Utils.isIdentifier(field.name()))
                throw new IllegalArgumentException("Invalid field name '" + field.name() + "'. Field names can contains only letters, digits and '_'");

            if (field.groupIndex != null && field.groupIndex <= 0)
                throw new IllegalArgumentException("Invalid group index in regexp format, 'groupIndex' must be greater than 0");

            if (field.groupIndex != null && field.groupIndex > groupCount) {
                throw new IllegalArgumentException("Invalid group index in regexp format, 'groupIndex' is greater than regexp group count ("
                        + field.groupIndex + " > " + groupCount + ')');
            }

            if (!usedGroups.add(field.groupIndex == null ? field.name() : field.groupIndex)) {
                throw new IllegalArgumentException("Two fields has reference to same regexp group: " + field.groupIndex);
            }
        }

        if (dateFieldIdx != null) {
            if (dateFieldIdx >= fields.length)
                throw new IllegalArgumentException();

            if (datePattern == null)
                throw new IllegalArgumentException();

            createDateFormatter(); // validate date format
        }
        else {
            if (datePattern != null)
                throw new IllegalArgumentException("'datePattern' argument must be null if 'dateField' is null");
        }
    }

    public String getDatePattern() {
        return datePattern;
    }

    public FieldDescriptor getDateField() {
        if (dateFieldIdx == null)
            return null;

        return fields[dateFieldIdx];
    }

    @Override
    public LogReader createReader() {
        return new RegexReader();
    }

    @Override
    public FieldDescriptor[] getFields() {
        return fields;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public boolean hasFullDate() {
        return dateFieldIdx != null;
    }

    private class RegexReader extends LogReader {

        private String s;
        private long start;
        private long end;
        private boolean hasMore;

        private SimpleDateFormat dateFormat;

        private final Charset charset = RegexLogFormat.this.charset == null ? Charset.defaultCharset() : RegexLogFormat.this.charset;

        private final int[] fields = new int[RegexLogFormat.this.fields.length * 2];

        @Override
        public boolean parseRecord(byte[] data, int offset, int length, long start, long end) {
            String s = new String(data, offset, length, charset);

            Matcher matcher = getPattern().matcher(s);
            if (!matcher.matches())
                return false;

            this.s = s;
            this.start = start;
            this.end = end;
            hasMore = length < end - start;

            for (int fieldIndex = 0; fieldIndex < RegexLogFormat.this.fields.length; fieldIndex++) {
                RegexpField field = RegexLogFormat.this.fields[fieldIndex];

                Integer groupIdx = field.groupIndex;

                int groupStart;

                if (groupIdx != null) {
                    groupStart = matcher.start(groupIdx);
                } else {
                    groupStart = matcher.start(field.name());
                }

                if (groupStart >= 0) {
                    fields[fieldIndex * 2] = groupStart;

                    if (groupIdx != null) {
                        fields[fieldIndex * 2 + 1] = matcher.end(groupIdx);
                    } else {
                        fields[fieldIndex * 2 + 1] = matcher.end(field.name());
                    }
                }
                else {
                    fields[fieldIndex * 2] = -1;
                    fields[fieldIndex * 2 + 1] = -1;
                }
            }

            return true;
        }

        @Override
        public boolean canAppendTail() {
            return fieldToExtend >= 0;
        }

        @Override
        public void appendTail(byte[] data, int offset, int length, long realLength) {
            if (s == null || fieldToExtend < 0)
                throw new IllegalStateException();

            if (length == 0)
                return;

            end += realLength;

            if (hasMore)
                return;

            if (fields[fieldToExtend * 2] == -1)
                throw new IllegalStateException();

            if (fields[fieldToExtend * 2 + 1] != s.length())
                throw new IllegalStateException();

            s = s + new String(data, offset, length, charset);
            fields[fieldToExtend * 2 + 1] += length;
        }

        @Override
        public boolean hasParsedRecord() {
            return s != null;
        }

        @Override
        public void clear() {
            s = null;
        }

        @Override
        public Record buildRecord() {
            if (s == null)
                throw new IllegalStateException();

            long time = 0;

            if (dateFieldIdx != null) {
                if (fields[dateFieldIdx] >= 0) {
                    if (dateFormat == null)
                        dateFormat = createDateFormatter();

                    Date date = dateFormat.parse(s, new ParsePosition(fields[dateFieldIdx * 2]));
                    if (date != null)
                        time = date.getTime();
                }
            }

            Record res = new Record(s, time, start, end, hasMore, fields.clone());

            s = null;

            return res;
        }
    }

    public static class RegexpField extends DefaultFieldDesciptor {

        private final Integer groupIndex;

        public RegexpField(@Nonnull String name) {
            this(name, null, null);
        }

        public RegexpField(@Nonnull String name, Integer groupIndex) {
            this(name, groupIndex, null);
        }

        public RegexpField(@Nonnull String name, Integer groupIndex, @Nullable String type) {
            super(name, type);
            this.groupIndex = groupIndex;
        }
    }

    public static RegexpField field(@Nonnull String name, @Nullable String type) {
        return field(name, type, null);
    }

    public static RegexpField field(@Nonnull String name, @Nullable String type, @Nullable Integer groupIndex) {
        return new RegexpField(name, groupIndex, type);
    }
}
