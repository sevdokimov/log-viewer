package com.logviewer.data2.net.server;

import com.logviewer.data2.FileAttributes;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.utils.Destroyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LogWatcherTask implements RemoteTask<FileAttributes> {

    private static final Logger LOG = LoggerFactory.getLogger(LogWatcherTask.class);

    public static final long DELAY = 200;

    private final String path;

    private Destroyer closeable;

    public LogWatcherTask(String path) {
        this.path = path;
    }

    @Override
    public void start(@NonNull RemoteTaskContext<FileAttributes> ctx) {
        Path path = Paths.get(this.path);
        try {
            closeable = ctx.getLogService().getFileWatcherService().watchDirectory(path.getParent(), changedDirs -> {
                if (changedDirs.contains(path)) {
                    ctx.getLogService().getTimer().scheduleTask(this, () -> {
                        FileAttributes attr;

                        try {
                            attr = FileAttributes.fromPath(path);
                        } catch (IOException e) {
                            LOG.error("Failed to read file attributes", e);
                            return;
                        }

                        ctx.send(attr);
                    }, DELAY);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cancel() {
        if (closeable != null)
            closeable.close();
    }
}
