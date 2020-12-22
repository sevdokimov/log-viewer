package com.logviewer.data2;

import com.logviewer.utils.Destroyer;
import com.logviewer.utils.Pair;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FileWatcherService implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(FileWatcherService.class);

    static final String THREAD_NAME = "file-watcher-thread";

    private Thread watcherThread;

    private WatchService watchService;

    private final Map<Path, Pair<WatchKey, List<WatcherDestroyer>>> listeners = new HashMap<>();

    public Destroyer watchDirectory(@NonNull Path path, @NonNull Consumer<List<Path>> listener) throws IOException {
        Path dir = path.toAbsolutePath();

        if (Files.exists(dir) && !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("path must be a directory: " + dir);
        }

        synchronized (this) {
            if (watchService == null)
                watchService = dir.getFileSystem().newWatchService();

            Pair<WatchKey, List<WatcherDestroyer>> pair = listeners.get(dir);
            if (pair == null) {
                WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                pair = Pair.of(key, new ArrayList<>());
                listeners.put(dir, pair);

                if (LOG.isDebugEnabled())
                    LOG.debug("Started watching {}", dir);
            }

            WatcherDestroyer closer = new WatcherDestroyer(dir, listener);

            pair.getSecond().add(closer);

            if (watcherThread == null) {
                watcherThread = new Thread(this::doWatch, THREAD_NAME);
                watcherThread.start();
            }

            return closer;
        }
    }

    public synchronized List<Path> watchedDirectories() {
        return new ArrayList<>(listeners.keySet());
    }

    private void doWatch() {
        try {
            while (true) {
                WatchKey key = watchService.take();

                if (!key.isValid())
                    continue;

                List<Path> paths = new ArrayList<>();

                Path dir = (Path) key.watchable();

                for (WatchEvent<?> event : key.pollEvents()) {
                    paths.add(dir.resolve((Path) event.context()));
                }

                List<WatcherDestroyer> listeners;

                synchronized (this.listeners) {
                    Pair<WatchKey, List<WatcherDestroyer>> pair = this.listeners.get(dir);
                    if (pair == null) {
                        LOG.error("Unregistered path: {}", dir);
                        key.cancel();
                        continue;
                    }

                    listeners = new ArrayList<>(pair.getSecond());
                }

                if (LOG.isDebugEnabled())
                    LOG.debug("Listeners invoked for {}", dir);

                for (WatcherDestroyer watcherDestroyer : listeners) {
                    try {
                        watcherDestroyer.listener.accept(paths);
                    } catch (Throwable e) {
                        if (Thread.currentThread().isInterrupted())
                            break;

                        LOG.error("Failed to invoke listener for dir: {}", dir, e);
                    }
                }

                key.reset();
            }
        } catch (InterruptedException | ClosedWatchServiceException ignored) {

        }
    }

    @Override
    public void destroy() {
        if (watchService != null) {
            Utils.closeQuietly(watchService);
            watchService = null;
        }

        if (watcherThread != null) {
            watcherThread.interrupt();
            try {
                watcherThread.join(1, Thread.MIN_PRIORITY);
            } catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
        }
    }

    private class WatcherDestroyer implements Destroyer {
        private final Path dir;
        private final Consumer<List<Path>> listener;

        WatcherDestroyer(Path dir, Consumer<List<Path>> listener) {
            this.dir = dir;
            this.listener = listener;
        }

        @Override
        public void close() {
            synchronized (FileWatcherService.this) {
                Pair<WatchKey, List<WatcherDestroyer>> p = listeners.get(dir);
                if (p == null)
                    return;

                p.getSecond().remove(this);
                if (p.getSecond().isEmpty()) {
                    p.getFirst().cancel();
                    listeners.remove(dir);

                    if (LOG.isDebugEnabled())
                        LOG.debug("Stopped watching {}", dir);

                    if (listeners.isEmpty()) {
                        if (watcherThread != null) {
                            watcherThread.interrupt();
                            watcherThread = null;
                        }
                    }
                }
            }
        }
    }
}
