package com.logviewer.data2.net;

import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.LogService;

import javax.annotation.Nonnull;

public class NotConnectedLogView extends BrokenLog {

    private final LogPath logPath;

    public NotConnectedLogView(@Nonnull LogPath logPath, Throwable exception) {
        super(exception);

        assert logPath.getNode() != null;

        this.logPath = logPath;
    }

    @Override
    public String getId() {
        return "!" + logPath.getNode() + "/" + logPath.getFile();
    }

    @Override
    public LogPath getPath() {
        return logPath;
    }

    @Override
    public String getHostname() {
        return logPath.getNode().toString();
    }

    @Override
    public LogFormat getFormat() {
        return LogService.DEFAULT_FORMAT;
    }
}
