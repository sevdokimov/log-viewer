package com.logviewer.formats;

import com.logviewer.data2.*;
import com.logviewer.formats.utils.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public class DefaultFieldSet {

    private final Charset charset;

    private final LvLayoutNode[] layout;

    private final boolean canAppendTail;

    private final LogFormat.FieldDescriptor[] fields;
    private final int[] fieldIndex;

    private final int dateNodeIndex;

    public DefaultFieldSet(@Nullable Charset charset, LvLayoutNode ... layout) {
        this(charset, canAppendTail(layout), layout);
    }

    public DefaultFieldSet(@Nullable Charset charset, boolean canAppendTail, LvLayoutNode ... layout) {
        this.charset = charset == null ? Charset.defaultCharset() : charset;
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

    @Nonnull
    public Charset getEncoding() {
        return charset;
    }

    @Nonnull
    public LogReader createReader() {
        return new LogReaderImpl();
    }

    public boolean hasFullDate() {
        return dateNodeIndex >= 0;
    }

    public static boolean canAppendTail(@Nonnull LvLayoutNode[] nodes) {
        if (nodes.length == 0)
            return false;

        LvLayoutNode lastNode = nodes[nodes.length - 1];

        return lastNode instanceof LvLayoutStretchNode;
    }

    private class LogReaderImpl extends LogReader {

        private final LvLayoutNode[] layoutCopy;

        private final int[] fieldOffset = new int[fields.length * 2];

        private final LongSupplier dateExtractor;

        private final StretchField[] stretchFields;

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
            stretchFields = new StretchField[layoutCopy.length];
        }

        @Nullable
        private LongSupplier createDateExtractor() {
            if (dateNodeIndex < 0)
                return null;

            LvLayoutDateNode dateNode = (LvLayoutDateNode) layoutCopy[dateNodeIndex];

            return () -> {
                int dateStart = fieldOffset[dateNodeIndex * 2];
                if (dateStart < 0)
                    return -1;

                return dateNode.getCurrentDate();
            };
        }

        @Override
        public boolean parseRecord(byte[] data, int offset, int length, long start, long end) {
            String s = new String(data, offset, length, charset);

            int idx = 0;
            int endStr = s.length();

            int stretchFieldSize = 0;

            mainLoop:
            for (int i = 0; ; ) {
                int nextIdx;

                if (i == layoutCopy.length) {
                    if (idx == endStr)
                        break;

                    nextIdx = LvLayoutNode.PARSE_FAILED;
                } else {
                    LvLayoutNode part = layoutCopy[i];

                    if (part.removeSpacesBefore()) {
                        while (idx < endStr && s.charAt(idx) == ' ') {
                            idx++;
                        }
                    }

                    nextIdx = part.parse(s, idx, endStr);
                }

                if (nextIdx == LvLayoutNode.PARSE_FAILED) {
                    while (true) {
                        if (stretchFieldSize == 0)
                            return false;

                        StretchField stretchField = stretchFields[stretchFieldSize - 1];
                        if (++stretchField.endPosition < endStr) {
                            idx = stretchField.endPosition;
                            i = stretchField.partIndex + 1;

                            if (layoutCopy[i] instanceof LvLayoutNodeSearchable) {
                                int searchNext = ((LvLayoutNodeSearchable)layoutCopy[i]).search(s, idx, endStr);
                                if (searchNext >= 0) {
                                    stretchField.endPosition = searchNext;
                                    idx = searchNext;
                                    continue mainLoop;
                                }
                            } else {
                                continue mainLoop;
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
                    if (nextIdx < 0) {
                        int minSkip = -(nextIdx + 1);

                        StretchField stretchField = new StretchField(i, idx, idx + minSkip);
                        assert stretchField.endPosition <= endStr;

                        stretchFields[stretchFieldSize++] = stretchField;

                        i++;

                        if (i == layoutCopy.length) {
                            stretchField.endPosition = endStr;
                            idx = endStr;
                            break;
                        }

                        idx = stretchField.endPosition;

                        if (layoutCopy[i] instanceof LvLayoutNodeSearchable) {
                            int searchNext = ((LvLayoutNodeSearchable)layoutCopy[i]).search(s, idx, endStr);
                            if (searchNext >= 0) {
                                stretchField.endPosition = searchNext;
                                idx = searchNext;
                            } else {
                                stretchField.endPosition = endStr; // not found
                                idx = endStr;
                            }
                        }

                        continue;
                    }

                    if (fieldIdx >= 0) {
                        fieldOffset[fieldIdx * 2] = idx;
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
                StretchField stretchField = stretchFields[i];

                int fieldIdx = fieldIndex[stretchField.partIndex];

                if (fieldIdx >= 0) {
                    fieldOffset[fieldIdx * 2] = stretchField.startPosition;
                    fieldOffset[fieldIdx * 2 + 1] = stretchField.endPosition;
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

            s = s + new String(data, offset, length, charset);
            fieldOffset[lastFieldOffset + 1] += length;
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

            if (dateExtractor != null) {
                time = dateExtractor.getAsLong();
            }

            Record res = new Record(s, time, start, end, hasMore, fieldOffset.clone());

            s = null;

            return res;
        }
    }

    private static class StretchField {
        int partIndex;

        int startPosition;

        int endPosition;

        public StretchField(int partIndex, int startPosition, int endPosition) {
            this.partIndex = partIndex;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }
    }
}
