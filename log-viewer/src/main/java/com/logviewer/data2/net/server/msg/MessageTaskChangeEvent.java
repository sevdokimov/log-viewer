package com.logviewer.data2.net.server.msg;

import com.logviewer.data2.net.server.Message;
import com.logviewer.data2.net.server.api.RemoteTask;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class MessageTaskChangeEvent implements Message {

    private final long taskId;

    private final Consumer<RemoteTask> modifier;

    /**
     *
     * @param taskId Id of task.
     * @param modifier Closure to modify task or {@code null} to cancel the task.
     */
    public MessageTaskChangeEvent(long taskId, @Nullable Consumer<RemoteTask> modifier) {
        this.taskId = taskId;
        this.modifier = modifier;
    }

    public long getTaskId() {
        return taskId;
    }

    public Consumer<RemoteTask> getModifier() {
        return modifier;
    }
}
