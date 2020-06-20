package com.logviewer.data2;

import com.logviewer.data2.net.RemoteNodeService;
import com.logviewer.data2.net.server.LogWatcherTask;
import com.logviewer.data2.net.server.api.RemoteTaskController;
import com.logviewer.utils.Destroyer;
import com.logviewer.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RemoteLogChangeListenerService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteLogChangeListenerService.class);

    private final RemoteNodeService remoteNodeService;

    private final Map<LogPath, Pair<Destroyer, List<ListenerDescription>>> map = new HashMap<>();

    public RemoteLogChangeListenerService(RemoteNodeService remoteNodeService) {

        this.remoteNodeService = remoteNodeService;
    }

    public Destroyer addListener(LogPath path, Consumer<FileAttributes> listener) {
        if (path.getNode() == null)
            throw new IllegalArgumentException();

        ListenerDescription description = new ListenerDescription(path, listener);

        synchronized (map) {
            Pair<Destroyer, List<ListenerDescription>> pair = map.computeIfAbsent(path, k -> {
                List<ListenerDescription> descriptions = new ArrayList<>();

                RemoteTaskController<LogWatcherTask> controller = remoteNodeService.startTask(k.getNode(),
                        new LogWatcherTask(k.getFile()),
                        (attr, e) -> {
                            if (e == null) {
                                if (LOG.isDebugEnabled())
                                    LOG.debug("Log change message received from LogWatcherTask [path={}]", path);

                                List<ListenerDescription> cloneDescriptions;

                                synchronized (map) {
                                    cloneDescriptions = new ArrayList<>(descriptions);
                                }

                                for (ListenerDescription destroyer1 : cloneDescriptions) {
                                    try {
                                        destroyer1.listener.accept(attr);
                                    } catch (Throwable ex) {
                                        LOG.error("Failed to notify listener", ex);
                                    }
                                }
                            }
                            else {
                                if (LOG.isDebugEnabled())
                                    LOG.debug("Exception from LogWatcherTask [path={}]", path, e);
                            }
                        });

                return Pair.of(controller::cancel, descriptions);
            });

            pair.getSecond().add(description);
        }

        return description;
    }

    private class ListenerDescription implements Destroyer {

        private final LogPath path;
        private final Consumer<FileAttributes> listener;

        ListenerDescription(LogPath path, Consumer<FileAttributes> listener) {
            this.path = path;
            this.listener = listener;
        }

        @Override
        public void close() {
            Destroyer realListenerDestroyer;

            synchronized (map) {
                Pair<Destroyer, List<ListenerDescription>> pair = map.get(path);
                if (pair == null)
                    return;

                if (!pair.getSecond().remove(this)) {
                    return;
                }

                if (pair.getSecond().size() > 0) {
                    return;
                }

                map.remove(path);
                realListenerDestroyer = pair.getFirst();
            }
            
            realListenerDestroyer.close();
        }
    }
}
