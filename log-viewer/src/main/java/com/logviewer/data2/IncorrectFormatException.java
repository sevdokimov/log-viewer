package com.logviewer.data2;

import java.io.IOException;

public class IncorrectFormatException extends IOException {

    private final String file;

    private final String format;

    private final long blockStart;

    private final long blockEnd;

    public IncorrectFormatException(String file, long blockStart, long blockEnd, LogFormat format) {
        super("Failed to parse file. Probably, the wrong log format is specified: " + file);

        this.file = file;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.format = format.getHumanReadableString();
    }

    public String getFile() {
        return file;
    }

    public long getBlockStart() {
        return blockStart;
    }

    public long getBlockEnd() {
        return blockEnd;
    }

    public String getFormat() {
        return format;
    }
}
