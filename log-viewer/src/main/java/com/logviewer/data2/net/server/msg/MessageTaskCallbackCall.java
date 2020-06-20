package com.logviewer.data2.net.server.msg;

import com.logviewer.data2.net.server.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MessageTaskCallbackCall implements Message {

    private final long taskId;

    private final Object event;

    private final Throwable error;

    private final boolean taskStopped;

    public MessageTaskCallbackCall(long taskId, @Nonnull Throwable error) {
        this.taskId = taskId;
        this.error = error;
        taskStopped = true;
        event = null;
    }

    public MessageTaskCallbackCall(long taskId, @Nullable Object event, boolean taskStopped) {
        this.taskId = taskId;
        this.event = event;
        this.error = null;
        this.taskStopped = taskStopped;
    }

    public Object getEvent() {
        return event;
    }

    public Throwable getError() {
        return error;
    }

    public long getTaskId() {
        return taskId;
    }

    public boolean isTaskStopped() {
        return taskStopped;
    }
}
