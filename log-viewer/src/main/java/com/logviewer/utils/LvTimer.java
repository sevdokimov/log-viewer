package com.logviewer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class LvTimer {

    private static final Logger LOG = LoggerFactory.getLogger(LvTimer.class);

    private final Map<Object, Boolean> uniqueTaskMap = new ConcurrentHashMap<>();

    private final Timer timer = new Timer("log-viewer-timer", true);

    public boolean scheduleTask(@NonNull Object key, @NonNull Runnable task, long delay) {
        if (uniqueTaskMap.putIfAbsent(key, true) == null) {
            boolean scheduled = false;

            try {
                timer.schedule(new TimerTaskImpl(() -> {
                    uniqueTaskMap.remove(key);
                    task.run();
                }), delay);

                scheduled = true;
            } finally {
                if (!scheduled)
                    uniqueTaskMap.remove(key);
            }

            return true;
        }

        return false;
    }

    public void cancel() {
        timer.cancel();
    }

    public TimerTask schedule(Runnable run, long delay) {
        TimerTaskImpl task = new TimerTaskImpl(run);
        timer.schedule(task, delay);
        return task;
    }

    public void schedule(TimerTask timerTask, long delay) {
        timer.schedule(timerTask, delay);
    }

    private static class TimerTaskImpl extends TimerTask {

        private final Runnable run;

        public TimerTaskImpl(Runnable run) {
            this.run = run;
        }

        @Override
        public void run() {
            try {
                run.run();
            } catch (Throwable e) {
                LOG.error("Failed to execute timer task", e);
            }
        }
    }
}
