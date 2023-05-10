package com.logviewer.web.dto.events;

public class LoadLogContentResponse extends BackendEvent {

    private final String logId;
    private final String text;
    private final int textLengthBytes;

    private final long recordStart;
    private final long offset;

    public LoadLogContentResponse(String logId, String text, int textLengthBytes, long recordStart, long offset) {
        this.logId = logId;
        this.text = text;
        this.textLengthBytes = textLengthBytes;
        this.recordStart = recordStart;
        this.offset = offset;
    }

    public String getLogId() {
        return logId;
    }

    public String getText() {
        return text;
    }

    public int getTextLengthBytes() {
        return textLengthBytes;
    }

    public long getRecordStart() {
        return recordStart;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public String getName() {
        return "onLoadLogContentResponse";
    }
}
