package com.logviewer.formats;

import com.logviewer.data2.DefaultFieldDesciptor;
import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import com.logviewer.data2.LogRecord;
import com.logviewer.formats.utils.LvLayoutClassNode;
import com.logviewer.formats.utils.LvLayoutCustomTypeNode;
import com.logviewer.formats.utils.LvLayoutDateNode;
import com.logviewer.formats.utils.LvLayoutNode;
import com.logviewer.formats.utils.LvLayoutNodeSearchable;
import com.logviewer.formats.utils.LvLayoutStretchNode;
import com.logviewer.utils.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public class DefaultFieldSet {

    private final Charset charset;

    private final Locale locale;

    private final LvLayoutNode[] layout;

    private final boolean canAppendTail;

    private final LogFormat.FieldDescriptor[] fields;
    private final int[] fieldIndex;

    private final Map<String, Integer> fieldNameIndexes = new LinkedHashMap<>();

    private final int dateNodeIndex;

    public DefaultFieldSet(@Nullable Charset charset, LvLayoutNode ... layout) {
        this(null, charset, canAppendTail(layout), layout);
    }

    public DefaultFieldSet(@Nullable Locale locale, @Nullable Charset charset, LvLayoutNode ... layout) {
        this(locale, charset, canAppendTail(layout), layout);
    }

    public DefaultFieldSet(@Nullable Locale locale, @Nullable Charset charset, boolean canAppendTail, LvLayoutNode ... layout) {
        this.charset = charset == null ? Charset.defaultCharset() : charset;
        this.locale = locale == null ? Locale.getDefault(Locale.Category.FORMAT) : locale;
        this.layout = layout.clone();
        this.canAppendTail = canAppendTail;

        List<LogFormat.FieldDescriptor> fields = new ArrayList<>();

        Map<String, AtomicInteger> existsField = new HashMap<>();

        int[] fieldIndex = new int[layout.length];

        int dateNodeIndex = -1;

        for (int i = 0; i < layout.length; i++) {
            LvLayoutNode node = layout[i];

            LogFormat.FieldDescriptor field = null;

            if (node instanceof LvLayoutDateNode) {
                if (((LvLayoutDateNode) node).isFull() && dateNodeIndex == -1)
                    dateNodeIndex = i;

                field = new DefaultFieldDesciptor(generateName(existsField, "date"), FieldTypes.DATE);
            } else if (node instanceof LvLayoutClassNode) {
                field = new DefaultFieldDesciptor(generateName(existsField, "logger"), FieldTypes.JAVA_CLASS);
            } else if (node instanceof LvLayoutCustomTypeNode) {
                LvLayoutCustomTypeNode sn = (LvLayoutCustomTypeNode) node;
                field = new DefaultFieldDesciptor(generateName(existsField, sn.getFieldName()), sn.getFieldType());
            }

            if (field != null) {
                fieldNameIndexes.put(field.name(), fields.size());
                fieldIndex[i] = fields.size();
                fields.add(field);
            } else {
                fieldIndex[i] = -1;
            }
        }

        this.dateNodeIndex = dateNodeIndex;
        this.fields = fields.toArray(LogFormat.FieldDescriptor.EMPTY_ARRAY);
        this.fieldIndex = fieldIndex;
    }

    public LvLayoutNode[] getLayout() {
        return layout;
    }

    private static String generateName(Map<String, AtomicInteger> fields, String name) {
        int nameIndex = fields.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
        if (nameIndex > 0)
            name = name + '_' + nameIndex;

        return name;
    }

    public LogFormat.FieldDescriptor[] getFields() {
        return fields;
    }

    @NonNull
    public Charset getEncoding() {
        return charset;
    }
    @NonNull
    public Locale getLocale() {
        return locale;
    }

    @NonNull
    public LogReader createReader() {
        return new LogReaderImpl();
    }

    public boolean hasFullDate() {
        return dateNodeIndex >= 0;
    }

    public static boolean canAppendTail(@NonNull LvLayoutNode[] nodes) {
        if (nodes.length == 0)
            return false;

        LvLayoutNode lastNode = nodes[nodes.length - 1];

        return lastNode instanceof LvLayoutStretchNode;
    }

    private static boolean spacesOnlyAfter(String s, int offset, int end) {
        for (; offset < end; offset++) {
            if (s.charAt(offset) != ' ')
                return false;
        }

        return true;
    }

    private class LogReaderImpl extends LogReader {

        private final LvLayoutNode[] layoutCopy;

        private final int[] fieldOffset = new int[fields.length * 2];

        private final LongSupplier dateExtractor;

        private final int[] stretchFields;

        private String s;
        private long start;
        private long end;
        private boolean hasMore;

        public LogReaderImpl() {
            layoutCopy = DefaultFieldSet.this.layout.clone();

            for (int i = 0; i < layoutCopy.length; i++) {
                layoutCopy[i] = layoutCopy[i].clone();
            }

            dateExtractor = createDateExtractor();
            stretchFields = new int[layoutCopy.length];
        }

        @Nullable
        private LongSupplier createDateExtractor() {
            if (dateNodeIndex < 0)
                return null;

            LvLayoutDateNode dateNode = (LvLayoutDateNode) layoutCopy[dateNodeIndex];
            int dateOffset = fieldIndex[dateNodeIndex] * 2;

            return () -> {
                int dateStart = fieldOffset[dateOffset];
                if (dateStart < 0)
                    return -1;

                return dateNode.getCurrentDate();
            };
        }

        @Override
        public boolean parseRecord(byte[] data, int offset, int length, long start, long end) {
            String s = new String(data, offset, length, charset);
            s = Utils.removeAsciiColorCodes(s);

            int idx = 0;
            int endStr = s.length();

            int stretchFieldSize = 0;

            mainLoop:
            for (int i = 0; ; ) {
                int nextIdx;

                if (i == layoutCopy.length) {
                    if (idx == endStr || spacesOnlyAfter(s, idx, endStr))
                        break;

                    nextIdx = LvLayoutNode.PARSE_FAILED;
                } else {
                    LvLayoutNode part = layoutCopy[i];

                    if (part.removeSpacesBefore()) {
                        while (idx < endStr && s.charAt(idx) == ' ') {
                            idx++;
                        }
                    }

                    if (part instanceof LvLayoutStretchNode) {
                        LvLayoutStretchNode stretchNode = (LvLayoutStretchNode) part;

                        if (stretchNode.reset(s, idx, endStr)) {
                            if (i + 1 < layoutCopy.length) {
                                stretchFields[stretchFieldSize++] = i;

                                idx = stretchNode.getEnd();
                                assert idx <= endStr;
                                i++;
                                continue;
                            }

                            if (stretchNode.grow(s, endStr, endStr)) {
                                assert stretchNode.getEnd() == endStr;
                                stretchFields[stretchFieldSize++] = i;

                                break; // parsing finished successfully
                            }
                        }

                        nextIdx = LvLayoutNode.PARSE_FAILED;
                    } else {
                        nextIdx = part.parse(s, idx, endStr);
                    }
                }

                if (nextIdx == LvLayoutNode.PARSE_FAILED) {
                    while (true) {
                        if (stretchFieldSize == 0)
                            return false;

                        i = stretchFields[stretchFieldSize - 1];
                        LvLayoutStretchNode stretchNode = (LvLayoutStretchNode) layoutCopy[i];

                        if (stretchNode.getEnd() < endStr) {
                            i++;
                            LvLayoutNode nextNode = layoutCopy[i];

                            if (!(nextNode instanceof LvLayoutNodeSearchable)) {
                                if (stretchNode.grow(s, stretchNode.getEnd() + 1, endStr)) {
                                    idx = stretchNode.getEnd();
                                    continue mainLoop;
                                }
                            } else {
                                idx = stretchNode.getEnd() + 1;
                                while (true) {
                                    int searchNext = ((LvLayoutNodeSearchable)nextNode).search(s, idx, endStr);
                                    if (searchNext < 0)
                                        break;

                                    if (!stretchNode.grow(s, searchNext, endStr))
                                        break;

                                    idx = stretchNode.getEnd();
                                    if (idx == searchNext)
                                        continue mainLoop;

                                    assert idx > searchNext;
                                }
                            }
                        }

                        stretchFieldSize--;
                    }
                }

                int fieldIdx = fieldIndex[i];

                if (nextIdx == LvLayoutNode.SKIP_FIELD) {
                    if (fieldIdx >= 0) {
                        fieldOffset[fieldIdx * 2] = -1;
                        fieldOffset[fieldIdx * 2 + 1] = -1;
                    }
                } else {
                    if (fieldIdx >= 0) {
                        fieldOffset[fieldIdx * 2] = layoutCopy[i].getValueStart(s, idx, endStr);
                        fieldOffset[fieldIdx * 2 + 1] = nextIdx;
                    }

                    idx = nextIdx;
                }

                i++;
            }

            this.s = s;
            this.start = start;
            this.end = end;
            hasMore = length < end - start;

            for (int i = 0; i < stretchFieldSize; i++) {
                int nodeIdx = stretchFields[i];
                LvLayoutStretchNode stretchField = (LvLayoutStretchNode) layoutCopy[nodeIdx];

                int fieldIdx = fieldIndex[nodeIdx];

                if (fieldIdx >= 0) {
                    fieldOffset[fieldIdx * 2] = stretchField.getValueStart(s, stretchField.getStart(), stretchField.getEnd());
                    fieldOffset[fieldIdx * 2 + 1] = stretchField.getEnd();
                }
            }

            return true;
        }

        @Override
        public boolean canAppendTail() {
            return canAppendTail;
        }

        @Override
        public void appendTail(byte[] data, int offset, int length, long realLength) {
            if (length == 0)
                return;

            end += realLength;

            if (hasMore)
                return;

            int lastFieldOffset = (fields.length - 1) * 2;

            if (fieldOffset[lastFieldOffset] == -1)
                throw new IllegalStateException();

            if (fieldOffset[lastFieldOffset + 1] != s.length())
                throw new IllegalStateException();

            s = s + Utils.removeAsciiColorCodes(new String(data, offset, length, charset));
            fieldOffset[lastFieldOffset + 1] = s.length();
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

            if (dateExtractor != null) {
                time = dateExtractor.getAsLong();
            }

            LogRecord res = new LogRecord(s, time, start, end, hasMore, fieldOffset.clone(), fieldNameIndexes);

            s = null;

            return res;
        }
    }
}
