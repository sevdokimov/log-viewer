package com.logviewer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class LvTimer extends Timer {

    private static final Logger LOG = LoggerFactory.getLogger(LvTimer.class);

    private final Map<Object, Boolean> uniqueTaskMap = new ConcurrentHashMap<>();

    public LvTimer() {
        super("log-viewer-timer", true);
    }

    public boolean scheduleTask(@Nonnull Object key, @Nonnull Runnable task, long delay) {
        if (uniqueTaskMap.putIfAbsent(key, true) == null) {
            schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        uniqueTaskMap.remove(key);
                        task.run();
                    } catch (Throwable e) {
                        LOG.error("Failed to execute scheduled task", e);
                    }
                }
            }, delay);

            return true;
        }

        return false;
    }
}
