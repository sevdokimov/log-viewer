package com.logviewer.web.session;

import com.logviewer.data2.FileAttributes;
import com.logviewer.data2.LogView;
import com.logviewer.utils.Destroyer;
import com.logviewer.utils.LvTimer;
import com.logviewer.web.dto.events.EventLogChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class LogChangeNotifier implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LogChangeNotifier.class);

    private static final long SEND_NOTIFICATION_DELAY = 700;

    private final Destroyer[] watcherCloser;

    private final SessionAdapter sender;

    private final LvTimer timer;

    private Map<String, FileAttributes> changedLogs;

    public LogChangeNotifier(LogView[] logs, SessionAdapter sender, LvTimer timer) {
        watcherCloser = Stream.of(logs).map(log -> log.addChangeListener(attr -> logChanged(log, attr)))
                .filter(Objects::nonNull)
                .toArray(Destroyer[]::new);
        this.sender = sender;

        this.timer = timer;
    }

    private void logChanged(LogView log, @Nullable FileAttributes attr) {
        boolean scheduleTask = false;

        synchronized (this) {
            if (changedLogs == null) {
                changedLogs = new HashMap<>();
                scheduleTask = true;
            }

            changedLogs.put(log.getId(), attr);
        }

        if (scheduleTask) {
            timer.schedule(() -> {
                Map<String, FileAttributes> changedLogs;

                synchronized (LogChangeNotifier.this) {
                    changedLogs = LogChangeNotifier.this.changedLogs;
                    LogChangeNotifier.this.changedLogs = null;
                }

                LOG.debug("Sending notification about log update: {}", changedLogs.keySet());

                sender.send(new EventLogChanged(changedLogs));
            }, SEND_NOTIFICATION_DELAY);
        }
    }

    @Override
    public void close() {
        if (watcherCloser != null) {
            for (Destroyer runnable : watcherCloser) {
                runnable.close();
            }
        }
    }
}
