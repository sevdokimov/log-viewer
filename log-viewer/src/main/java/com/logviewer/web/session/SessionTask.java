package com.logviewer.web.session;

import com.logviewer.data2.LogView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public abstract class SessionTask<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SessionTask.class);

    public static final int MAX_BATCH_SIZE = 50 * 1024;

    protected final SessionAdapter sender;

    protected final LogView[] logs;

    public SessionTask(SessionAdapter sender, LogView[] logs) {
        this.sender = sender;
        this.logs = logs;
    }

    public abstract void execute(BiConsumer<T, Throwable> consumer);

    public abstract void cancel();
}
