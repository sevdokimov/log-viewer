package com.logviewer.data2;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.data2.net.NotConnectedLogView;
import com.logviewer.data2.net.RemoteLog;
import com.logviewer.data2.net.RemoteNodeService;
import com.logviewer.formats.SimpleLogFormat;
import com.logviewer.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class LogService implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(LogService.class);

    public static final LogFormat DEFAULT_FORMAT = new SimpleLogFormat(Charset.defaultCharset());

    private final Map<Pair<Path, String>, Log> logs = new ConcurrentHashMap<>();

    private ExecutorService executor;

    @Autowired
    private FileWatcherService fileWatcherService;
    @Autowired
    private RemoteNodeService remoteNodeService;
    @Autowired
    private LvFileAccessManager accessManager;
    @Autowired
    private RemoteLogChangeListenerService remoteLogChangeListenerService;
    @Autowired
    private LvTimer timer;
    @Autowired(required = false)
    private List<LvFormatRecognizer> formatRecognizers = Collections.emptyList();

    @Override
    public void afterPropertiesSet() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8,
                20L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("log-service-%d")
                        .setUncaughtExceptionHandler((t, e) -> LOG.error("Unhandled error", e))
                        .build());
        executor.allowCoreThreadTimeOut(true);
        this.executor = executor;
    }

    public RemoteNodeService getRemoteNodeService() {
        return remoteNodeService;
    }

    public FileWatcherService getFileWatcherService() {
        return fileWatcherService;
    }

    public LvTimer getTimer() {
        return timer;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @Nullable
    public LogFormat getFormatByPath(@Nonnull Path path) {
        try {
            path = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException ignored) {

        } catch (IOException e) {
            LOG.warn("Failed to get canonical path from " + path, e);
        }

        for (LvFormatRecognizer formatRecognizer : formatRecognizers) {
            LogFormat formatName = formatRecognizer.getFormat(path);

            if (formatName != null)
                return formatName;
        }

        return null;
    }

    @Nonnull
    public CompletableFuture<Map<String, LogView>> openLogs(@Nonnull Collection<LogPath> paths) {
        Map<String, LogView> res = new LinkedHashMap<>();
        List<CompletableFuture<LogView>> remoteLogs = new ArrayList<>();

        for (LogPath logPath : paths) {
            if (logPath.getNode() == null) {
                Log log = openLog(logPath.getFile());
                res.put(log.getId(), log);
            }
            else {
                remoteLogs.add(openRemoteLog(logPath));
            }
        }

        if (remoteLogs.isEmpty())
            return CompletableFuture.completedFuture(res);

        return CompletableFuture.allOf(remoteLogs.toArray(new CompletableFuture[0])).thenApply(v -> {
            for (CompletableFuture<LogView> remoteLog : remoteLogs) {
                LogView logView = Utils.safeGet(remoteLog);
                res.putIfAbsent(logView.getId(), logView);
            }

            return res;
        });
    }

    @Nonnull
    public CompletableFuture<LogView> openRemoteLog(@Nonnull LogPath path) {
        assert path.getNode() != null;

        CompletableFuture<LogView> res = new CompletableFuture<>();

        remoteNodeService.getNodeConnection(path.getNode()).whenComplete(Wrappers.of(LOG, (conn, error) -> {
            if (error != null) {
                res.complete(new NotConnectedLogView(path, error));
            }
            else {
                conn.execute(new GetFormatAndIdTask(path.getFile())).whenComplete(Wrappers.of(LOG, (info, t) -> {
                    if (t != null) {
                        res.complete(new NotConnectedLogView(path, t));
                    }
                    else {
                        LogFormat format;

                        try {
                            format = LvGsonUtils.GSON.fromJson(info.getFirst(), LogFormat.class);
                        } catch (Throwable e1) {
                            LOG.error("Incorrect log format", e1);
                            res.complete(new NotConnectedLogView(path, e1));
                            return;
                        }

                        res.complete(new RemoteLog(path, format, info.getSecond(), info.getThird(), remoteNodeService,
                                remoteLogChangeListenerService));
                    }
                }));
            }
        }));

        return res;
    }

    @Nonnull
    public Log openLog(@Nonnull String pathStr) {
        Path path = normalizePath(Paths.get(pathStr));

        LogFormat format = getFormatByPath(path);

        return openLog0(path, format);
    }

    @Nonnull
    public Log openLog(@Nonnull String path, @Nullable LogFormat format) {
        return openLog(Paths.get(path), format);
    }

    @Nonnull
    public Log openLog(@Nonnull Path path, @Nullable LogFormat format) {
        return openLog0(normalizePath(path), format);
    }

    private Path normalizePath(Path path) {
        try {
            return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException ignored) {

        } catch (IOException e) {
            LOG.warn("Failed to get canonical path from " + path, e);
        }

        return path;
    }

    @Nonnull
    private Log openLog0(@Nonnull Path path, @Nullable LogFormat format) {
        LogFormat finalFormat = format == null ? DEFAULT_FORMAT : LvGsonUtils.copy(format);

        return logs.computeIfAbsent(Pair.of(path, Utils.getFormatHash(finalFormat)), p -> {
            return new Log(p.getFirst(), finalFormat, executor, timer, fileWatcherService, accessManager);
        });
    }

    @Override
    public void destroy() {
        executor.shutdownNow();

        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void reset() {
        logs.clear();
    }
}
