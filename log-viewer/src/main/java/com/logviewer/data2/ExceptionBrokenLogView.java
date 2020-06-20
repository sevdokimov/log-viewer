package com.logviewer.data2;

import com.logviewer.data2.net.BrokenLog;

public class ExceptionBrokenLogView extends BrokenLog {

    private final LogView delegate;

    public ExceptionBrokenLogView(LogView delegate, Throwable error) {
        super(error);

        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public LogPath getPath() {
        return delegate.getPath();
    }

    @Override
    public String getHostname() {
        return delegate.getHostname();
    }

    @Override
    public LogFormat getFormat() {
        return delegate.getFormat();
    }
}
