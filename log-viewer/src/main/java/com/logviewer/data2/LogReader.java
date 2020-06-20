package com.logviewer.data2;

public abstract class LogReader {

    public boolean parseRecord(BufferedFile.Line line) {
        return parseRecord(line.getBuf(), line.getBufOffset(), line.getDataLength(), line.getStart(), line.getEnd());
    }

    public abstract boolean parseRecord(byte[] data, int offset, int length, long start, long end);

    public abstract boolean canAppendTail();

    public abstract void appendTail(byte[] data, int offset, int length, long realLength);

    public abstract boolean hasParsedRecord();

    public abstract void clear();

    public abstract Record buildRecord();


}
