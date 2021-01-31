package com.logviewer.formats;

import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import com.logviewer.data2.Record;
import com.logviewer.utils.Utils;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

public class SimpleLogFormat implements LogFormat {

    private Charset charset;

    public SimpleLogFormat() {
        this(null);
    }

    public SimpleLogFormat(@Nullable Charset charset) {
        this.charset = charset;
    }

    @Override
    public LogReader createReader() {
        return new DefaultReader();
    }

    @Override
    public FieldDescriptor[] getFields() {
        return FieldDescriptor.EMPTY_ARRAY;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public boolean hasFullDate() {
        return false;
    }

    @Override
    public void validate() throws IllegalArgumentException {

    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    private class DefaultReader extends LogReader {

        private String s;
        private long start;
        private long end;
        private boolean hasMore;

        private final Charset charset = SimpleLogFormat.this.charset == null ? Charset.defaultCharset() : SimpleLogFormat.this.charset;

        @Override
        public boolean parseRecord(byte[] data, int offset, int length, long start, long end) {
            s = new String(data, offset, length, charset);

            this.start = start;
            this.end = end;
            hasMore = length < end - start;

            return true;
        }

        @Override
        public boolean canAppendTail() {
            return false;
        }

        @Override
        public void appendTail(byte[] data, int offset, int length, long realLength) {
            throw new UnsupportedOperationException();
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

            Record res = new Record(s, 0, start, end, hasMore, Utils.EMPTY_INT_ARRAY);

            s = null;

            return res;
        }
    }
}
