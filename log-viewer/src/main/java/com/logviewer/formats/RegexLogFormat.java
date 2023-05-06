package com.logviewer.formats;

import com.logviewer.data2.DefaultFieldDesciptor;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import com.logviewer.data2.LogRecord;
import com.logviewer.formats.utils.FastDateTimeParser;
import com.logviewer.utils.LvDateUtils;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class RegexLogFormat implements LogFormat, Cloneable {

    private Charset charset;

    private Locale locale;

    private final String regex;

    private final RegexField[] fields;

    private boolean dontAppendUnmatchedTextToLastField;

    private Integer dateFieldIdx;
    private String dateFieldName;
    private String datePattern;

    private transient volatile Pattern pattern;

    public RegexLogFormat(@NonNull String regex, RegexField... fields) {
        this(regex, null, null, fields);
    }

    public RegexLogFormat(@NonNull String regex,
                          @Nullable String datePattern, @Nullable String dateFieldName,
                          RegexField... fields) {
        this.regex = regex;
        this.fields = fields;

        this.datePattern = datePattern;

        if (dateFieldName != null) {
            if (Stream.of(fields).noneMatch(f -> f.name().equals(dateFieldName)))
                throw new IllegalArgumentException("Field not found: " + dateFieldName);

            this.dateFieldName = dateFieldName;
        }

        validate();
    }

    public boolean isDontAppendUnmatchedTextToLastField() {
        return dontAppendUnmatchedTextToLastField;
    }

    public RegexLogFormat setDontAppendUnmatchedTextToLastField(boolean dontAppendUnmatchedTextToLastField) {
        this.dontAppendUnmatchedTextToLastField = dontAppendUnmatchedTextToLastField;
        return this;
    }

    public RegexLogFormat setCharset(@Nullable Charset charset) {
        this.charset = charset;
        return this;
    }

    public RegexLogFormat setLocale(@Nullable Locale locale) {
        this.locale = locale;
        return this;
    }

    private Pattern getPattern() {
        Pattern res = pattern;

        if (res == null) {
            String regex = this.regex;
            if (regex == null || regex.isEmpty())
                throw new IllegalArgumentException("'regex' field is empty");

            try {
                res = Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid pattern [" + regex + "] " + e.getMessage(), e);
            }

            pattern = res;
        }

        return res;
    }

    public void validate() throws IllegalArgumentException {
        int groupCount = getPattern().matcher("").groupCount();

        Set<Object> usedGroups = new HashSet<>();

        for (RegexField field : fields) {
            if (field.name() == null || field.name().isEmpty())
                throw new IllegalArgumentException("Filed name can not be empty string");

            if (!Utils.isIdentifier(field.name()))
                throw new IllegalArgumentException("Invalid field name '" + field.name() + "'. Field names can contains only letters, digits and '_'");

            if (field.groupIndex != null && field.groupIndex <= 0)
                throw new IllegalArgumentException("Invalid group index in regex format, 'groupIndex' must be greater than 0");

            if (field.groupIndex != null && field.groupIndex > groupCount) {
                throw new IllegalArgumentException("Invalid group index in regex format, 'groupIndex' is greater than regex group count ("
                        + field.groupIndex + " > " + groupCount + ')');
            }

            if (!usedGroups.add(field.groupIndex == null ? field.name() : field.groupIndex)) {
                throw new IllegalArgumentException("Two fields has reference to same regex group: " + field.groupIndex);
            }
        }

        if (dateFieldIdx != null || dateFieldName != null) {
            if (dateFieldIdx != null) {
                if (dateFieldIdx >= fields.length)
                    throw new IllegalArgumentException("Invalid 'dateFieldIdx': " + dateFieldIdx + " >= " + fields.length);
            }
            if (dateFieldName != null) {
                if (Stream.of(fields).noneMatch(f -> f.name().equals(dateFieldName)))
                    throw new IllegalArgumentException("Invalid 'dateFieldName': no field with name \"" + dateFieldName + '"');
            }

            if (datePattern == null)
                throw new IllegalArgumentException("'dateFieldIdx' is specified, but 'datePattern' is null");

            if (!LvDateUtils.isDateFormatFull(new SimpleDateFormat(datePattern)))
                throw new IllegalArgumentException("Invalid date format. Format must include date and time");

            FastDateTimeParser.createFormatter(datePattern, null, null);// validate date format
        }
        else {
            if (datePattern != null)
                throw new IllegalArgumentException("'datePattern' argument must be null if 'dateField' is null");
        }
    }

    @Override
    public String getHumanReadableString() {
        return "regexp: " + regex;
    }

    public String getDatePattern() {
        return datePattern;
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
    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean hasFullDate() {
        return dateFieldIdx != null || dateFieldName != null;
    }

    private class RegexReader extends LogReader {

        private String s;
        private long start;
        private long end;
        private boolean hasMore;

        private BiFunction<String, ParsePosition, Supplier<Instant>> dateFormat;

        private final Charset charset = RegexLogFormat.this.charset == null ? Charset.defaultCharset() : RegexLogFormat.this.charset;

        private final int[] fields = new int[RegexLogFormat.this.fields.length * 2];

        private final Map<String, Integer> fieldNames = new LinkedHashMap<>();

        public RegexReader() {
            for (int i = 0; i < RegexLogFormat.this.fields.length; i++) {
                fieldNames.put(RegexLogFormat.this.fields[i].name(), i);
            }
        }

        @Override
        public boolean parseRecord(byte[] data, int offset, int length, long start, long end) {
            String s = new String(data, offset, length, charset);
            s = Utils.removeAsciiColorCodes(s);

            Matcher matcher = getPattern().matcher(s);
            if (!matcher.matches())
                return false;

            this.s = s;
            this.start = start;
            this.end = end;
            hasMore = length < end - start;

            for (int fieldIndex = 0; fieldIndex < RegexLogFormat.this.fields.length; fieldIndex++) {
                RegexField field = RegexLogFormat.this.fields[fieldIndex];

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
            return !dontAppendUnmatchedTextToLastField && fields.length > 0;
        }

        @Override
        public void appendTail(byte[] data, int offset, int length, long realLength) {
            if (s == null || dontAppendUnmatchedTextToLastField)
                throw new IllegalStateException();

            if (fields.length == 0)
                throw new IllegalStateException();

            if (length == 0)
                return;

            end += realLength;

            if (hasMore)
                return;

            int lastField = RegexLogFormat.this.fields.length - 1;

            if (fields[lastField * 2] == -1) {
                assert fields[lastField * 2 + 1] == -1;
                fields[lastField * 2] = s.length();
            } else {
                if (fields[lastField * 2 + 1] != s.length())
                    throw new IllegalStateException("Failed to append text to the last field, the last field '"
                            + RegexLogFormat.this.fields[lastField].name() +"' is not on the end of line");
            }

            s = s + Utils.removeAsciiColorCodes(new String(data, offset, length, charset));
            fields[lastField * 2 + 1] = s.length();
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
        public LogRecord buildRecord() {
            if (s == null)
                throw new IllegalStateException();

            long time = 0;

            Integer dateFieldIdx = RegexLogFormat.this.dateFieldIdx;
            if (dateFieldIdx == null && dateFieldName != null) {
                dateFieldIdx = fieldNames.get(dateFieldName);
            }

            if (dateFieldIdx != null) {
                if (fields[dateFieldIdx * 2] >= 0) {
                    if (dateFormat == null)
                        dateFormat = FastDateTimeParser.createFormatter(datePattern, locale, null);

                    Supplier<Instant> timestamp = dateFormat.apply(s, new ParsePosition(fields[dateFieldIdx * 2]));
                    if (timestamp != null) {
                        Instant instant = timestamp.get();
                        time = LvDateUtils.toNanos(instant);
                    }
                }
            }

            LogRecord res = new LogRecord(s, time, start, end, hasMore, fields.clone(), fieldNames);

            s = null;

            return res;
        }
    }

    public static class RegexField extends DefaultFieldDesciptor {

        private final Integer groupIndex;

        public RegexField(@NonNull String name) {
            this(name, null, null);
        }

        public RegexField(@NonNull String name, Integer groupIndex) {
            this(name, groupIndex, null);
        }

        public RegexField(@NonNull String name, Integer groupIndex, @Nullable String type) {
            super(name, type);
            this.groupIndex = groupIndex;
        }
    }

    public static RegexField field(@NonNull String name, @Nullable String type) {
        return field(name, type, null);
    }

    public static RegexField field(@NonNull String name, @Nullable String type, @Nullable Integer groupIndex) {
        return new RegexField(name, groupIndex, type);
    }
}
