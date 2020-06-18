package com.logviewer.data2.net.server.msg;

import com.logviewer.data2.net.server.Message;
import com.logviewer.data2.net.server.api.RemoteTask;

public class MessageStartTask implements Message {

    private final long taskId;

    private final RemoteTask task;

    public MessageStartTask(long taskId, RemoteTask task) {
        this.taskId = taskId;
        this.task = task;
    }

    public long getTaskId() {
        return taskId;
    }

    public RemoteTask getTask() {
        return task;
    }
}
