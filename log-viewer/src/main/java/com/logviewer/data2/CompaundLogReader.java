package com.logviewer.data2;

import org.springframework.lang.NonNull;

import java.util.function.BiFunction;

public class CompaundLogReader extends LogReader {

    private final LogReader[] readers;

    private final BiFunction<LogRecord, Integer, LogRecord> transformer;

    private int activeReader = -1;

    public CompaundLogReader(@NonNull LogReader[] readers, @NonNull BiFunction<LogRecord, Integer, LogRecord> transformer) {
        this.transformer = transformer;
        if (readers.length == 0)
            throw new IllegalArgumentException();
        
        this.readers = readers;
    }

    @Override
    public boolean parseRecord(byte[] data, int offset, int length, long start, long end) {
        for (activeReader = 0; activeReader < readers.length; activeReader++) {
            if (readers[activeReader].parseRecord(data, offset, length, start, end))
                return true;
        }

        activeReader = -1;
        return false;
    }

    @Override
    public boolean canAppendTail() {
        return readers[activeReader].canAppendTail();
    }

    @Override
    public void appendTail(byte[] data, int offset, int length, long realLength) {
        readers[activeReader].appendTail(data, offset, length, realLength);
    }

    @Override
    public void clear() {
        if (activeReader >= 0) {
            readers[activeReader].clear();
            activeReader = -1;
        }
    }

    @Override
    public boolean hasParsedRecord() {
        if (activeReader >= 0) {
            return readers[activeReader].hasParsedRecord();
        }

        return false;
    }

    @Override
    public LogRecord buildRecord() {
        LogRecord res = readers[activeReader].buildRecord();
        res = transformer.apply(res, activeReader);
        activeReader = -1;
        return res;
    }
}
