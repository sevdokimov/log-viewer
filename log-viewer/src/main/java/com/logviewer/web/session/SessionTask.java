package com.logviewer.web.session;

import com.logviewer.data2.LogView;
import org.springframework.lang.NonNull;

import java.util.function.BiConsumer;

public abstract class SessionTask<T> {

    public static final int MAX_BATCH_SIZE = 50 * 1024;

    protected final LogView[] logs;

    public SessionTask(@NonNull LogView[] logs) {
        this.logs = logs;
    }

    public abstract void execute(BiConsumer<T, Throwable> consumer);

    public abstract void cancel();
}
