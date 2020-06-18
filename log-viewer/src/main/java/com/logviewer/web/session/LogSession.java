package com.logviewer.web.session;

import com.logviewer.api.*;
import com.logviewer.data2.*;
import com.logviewer.domain.Permalink;
import com.logviewer.filters.NotPredicate;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.filters.SubstringPredicate;
import com.logviewer.filters.ViewFilterPredicate;
import com.logviewer.utils.Utils;
import com.logviewer.utils.Wrappers;
import com.logviewer.web.dto.events.*;
import com.logviewer.web.rmt.Remote;
import com.logviewer.web.session.tasks.LoadNextResponse;
import com.logviewer.web.session.tasks.LoadRecordTask;
import com.logviewer.web.session.tasks.SearchPattern;
import com.logviewer.web.session.tasks.SearchTask;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class LogSession {

    private static final Logger LOG = LoggerFactory.getLogger(LogSession.class);

    public static final Exception NO_DATE_EXCEPTION = new Exception("No date field, log cannot be merged");

    private final SessionAdapter sender;

    @Autowired
    private LogService logService;
    @Autowired
    private FavoriteLogService favoriteLogService;
    @Autowired
    private LvFilterStorage filterStorage;
    @Autowired
    private LvPermalinkStorage permalinkStorage;
    @Autowired(required = false)
    private List<LvFilterPanelStateProvider> filterSetProviders = Collections.emptyList();
    @Autowired(required = false)
    private List<LvUiConfigurer> uiConfigurers = Collections.emptyList();
    @Autowired(required = false)
    private List<LvPathResolver> pathResolvers = Collections.emptyList();

    private final List<SessionTask<?>> executions = new LinkedList<>();

    private ViewFilterPredicate filter;
    private long stateVersion;

    private LogView[] logs;
    private LogChangeNotifier logChangeNotifier;

    private static volatile Config defaultConfig;

    public LogSession(SessionAdapter sessionAdapter) {
        this.sender = sessionAdapter;
    }

    public LogView[] getLogs() {
        return logs;
    }

    private void initFilters(@Nonnull String[] paths, @Nullable String filterStateName, @Nullable String filterState,
                             boolean isInitByPermalink) {
        Set<LogPath> logPaths = parsePathParameter(paths);

        Map<String, LogView> logsMap = Utils.safeGet(logService.openLogs(logPaths));
        logs = logsMap.values().toArray(new LogView[0]);

        if (logs.length > 1) {
            for (int i = 0; i < logs.length; i++) {
                if (logs[i].isConnected() && !logs[i].getFormat().hasFullDate()) {
                    Throwable e = Utils.safeGet(logs[i].tryRead());
                    if (e == null)
                        e = NO_DATE_EXCEPTION;

                    logs[i] = new ExceptionBrokenLogView(logs[i], e);
                }
            }
        }

        logChangeNotifier = new LogChangeNotifier(logs, sender, logService.getTimer());

        Map<String, String> globalSavedFilters = new LinkedHashMap<>();
        for (LvFilterPanelStateProvider filterSetProvider : filterSetProviders) {
            globalSavedFilters.putAll(filterSetProvider.getFilterSets());
        }

        if (filterState == null && filterStateName != null) {
            filterState = globalSavedFilters.get(filterStateName);
        }

        sender.send(new EventSetViewState(logs, createConfigProps(), favoriteLogService, globalSavedFilters, filterState, isInitByPermalink));
    }

    private Config createConfigProps() {
        Config res = LogSession.defaultConfig;
        if (res == null) {
            res = ConfigFactory.parseResourcesAnySyntax(LogSession.class.getClassLoader(), "log-viewer-ui");
            LogSession.defaultConfig = res;
        }

        for (LvUiConfigurer lvUiConfigurer : uiConfigurers) {
            Config uiConfig = lvUiConfigurer.getUiConfig();
            if (uiConfig != null)
                res = uiConfig.withFallback(res);
        }

        return res.resolve(ConfigResolveOptions.noSystem());
    }

    @Remote
    public synchronized void initPermalink(int recordCount, @Nonnull String linkHash) {
        if (stateVersion != 0)
            throw new IllegalStateException(String.valueOf(stateVersion));

        Permalink permalink;

        try {
            permalink = permalinkStorage.load(linkHash);
        } catch (IOException e) {
            sender.send(new EventBrokenLink());
            return;
        }

        stateVersion = 1;

        initFilters(permalink.getPaths(), permalink.getSavedFiltersName(), permalink.getFilterState(), true);
        if (logs.length == 0)
            return;

        List<RecordPredicate> filters = new ArrayList<>();
        if (permalink.getFiltersFromFilterPanel() != null)
            Collections.addAll(filters, permalink.getFiltersFromFilterPanel());

        if (permalink.isHideUnmatched() && permalink.getSearchPattern() != null) {
            filters.add(new NotPredicate(new SubstringPredicate(permalink.getSearchPattern())));
        }

        filter = new ViewFilterPredicate(filters.toArray(new RecordPredicate[0]));

        CompletableFuture<LoadNextResponse> execution = execute(new LoadRecordTask(sender, logs, recordCount, filter, permalink.getOffset(),
                false, permalink.getHashes()));

        execution.whenComplete(new LogExecutionHandler<LoadNextResponse>() {

            @Override
            public void accept(LoadNextResponse res, Throwable e) {
                synchronized (LogSession.this) {
                    if (initStateVersion != stateVersion)
                        return;

                    if (res.getStatuses().values().stream().map(Status::getError).anyMatch(it -> it instanceof LogCrashedException)) {
                        sender.send(new EventBrokenLink());
                        return;
                    }

                    super.accept(res, e);
                }

            }

            @Override
            protected void handle(LoadNextResponse res) {
                sender.send(new EventInitByPermalink(res.getStatuses(), stateVersion, res, permalink));

                loadNext(permalink.getOffset(), true, recordCount, permalink.getHashes(), stateVersion);
            }
        });

    }

    @Remote
    public synchronized void init(@Nonnull String[] paths,
                                  @Nullable String savedFiltersName, @Nullable String filterStateHash) {
        if (stateVersion != 0)
            throw new IllegalStateException(String.valueOf(stateVersion));

        stateVersion = 1;

        String filterState = null;

        if (filterStateHash != null) {
            filterState = filterStorage.loadFilterStateByHash(filterStateHash);
        }

        initFilters(paths, savedFiltersName, filterState, false);
    }

    private ViewFilterPredicate andPredicate(@Nullable RecordPredicate[] filter) {
        if (filter == null || filter.length == 0)
            return null;

        return new ViewFilterPredicate(filter);
    }

    private boolean updateStateVersionAndFilters(long version, RecordPredicate[] filter) {
        if (stateVersion >= version)
            return false;

        stateVersion = version;

        for (SessionTask<?> execution : executions) {
            execution.cancel();
        }

        executions.clear();

        this.filter = andPredicate(filter);

        return true;
    }

    @Remote
    public synchronized void scrollToEdge(int recordCount, long stateVersion, @Nullable RecordPredicate[] filter, boolean isScrollToBegin) {
        if (!updateStateVersionAndFilters(stateVersion, filter))
            return;

        Position pos = null;

        if (isScrollToBegin) {
            String logId = logs.length == 1 ? logs[0].getId() : "";
            pos = new Position(logId, 0, 0);
        }

        CompletableFuture<LoadNextResponse> future = execute(new LoadRecordTask(sender, logs, recordCount, this.filter,
                pos, !isScrollToBegin, null));

        future.whenComplete(new LogExecutionHandler<LoadNextResponse>() {
            @Override
            protected void handle(LoadNextResponse res) {
                sender.send(new EventScrollToEdgeResponse(res.getStatuses(), stateVersion, res, isScrollToBegin));
            }
        });
    }

    @Remote
    public synchronized void loadingDataAfterFilterChangedSingle(int recordCount, long stateVersion, @Nullable RecordPredicate[] filter) {
        if (!updateStateVersionAndFilters(stateVersion, filter))
            return;

        LoadRecordTask task = new LoadRecordTask(sender, logs, recordCount, this.filter, null, true, null);

        CompletableFuture<LoadNextResponse> ex = execute(task);

        ex.whenComplete(new LogExecutionHandler<LoadNextResponse>() {
            @Override
            protected void handle(LoadNextResponse res) {
                sender.send(new EventResponseAfterFilterChangedSingle(res.getStatuses(), stateVersion, res));
            }
        });
    }

    @Remote
    public synchronized void loadingDataAfterFilterChanged(int topRecordCount, int bottomRecordCount,
                                                           long stateVersion, Map<String, String> hashes, @Nullable RecordPredicate[] filter,
                                                           Position start) {
        if (!updateStateVersionAndFilters(stateVersion, filter))
            return;

        LoadRecordTask topLoadTask = new LoadRecordTask(sender, logs, topRecordCount, this.filter, start, true, hashes);
        LoadRecordTask bottomLoadTask = new LoadRecordTask(sender, logs, bottomRecordCount, this.filter, start, false, hashes);

        CompletableFuture<LoadNextResponse> topLoadFut = execute(topLoadTask);
        CompletableFuture<LoadNextResponse> bottomLoadFut = execute(bottomLoadTask);

        BiConsumer<LoadNextResponse, Throwable> errorConsumer = Wrappers.of(LOG, new BiConsumer<LoadNextResponse, Throwable>() {
            private final AtomicBoolean responseSend = new AtomicBoolean();

            @Override
            public void accept(LoadNextResponse loadNextResponse, Throwable throwable) {
                if (throwable == null || !responseSend.compareAndSet(false, true))
                    return;

                topLoadTask.cancel();
                bottomLoadTask.cancel();

                synchronized (LogSession.this) {
                    handleTaskError(throwable);
                }
            }
        });

        topLoadFut.whenComplete(errorConsumer);
        bottomLoadFut.whenComplete(errorConsumer);

        topLoadFut.thenAcceptBoth(bottomLoadFut, (top, bottom) -> {
            if (stateVersion == this.stateVersion) {
                sender.send(new EventResponseAfterFilterChanged(bottom.getStatuses(), stateVersion, top, bottom));
            }
        });
    }

    @Remote
    public synchronized void loadNext(Position start, boolean backward, int recordCount, Map<String, String> hashes, long stateVersion) {
        if (this.stateVersion > stateVersion)
            return;

        if (this.stateVersion < stateVersion)
            throw new IllegalStateException("backend_stateVersion=" + this.stateVersion + ", but UI_stateVersion=" + stateVersion);

        for (SessionTask<?> task : executions) {
            if (task instanceof LoadRecordTask) {
                LoadRecordTask t = (LoadRecordTask) task;

                if (Objects.equals(t.getStart(), start) && t.isBackward() == backward && Objects.equals(t.getHashes(), hashes))
                    return; // duplicated request
            }
        }

        CompletableFuture<LoadNextResponse> execution = execute(new LoadRecordTask(sender, logs, recordCount, filter, start, backward, hashes));
        execution.whenComplete(new LogExecutionHandler<LoadNextResponse>() {
            @Override
            protected void handle(LoadNextResponse res) {
                sender.send(new EventNextDataLoaded(res.getStatuses(), stateVersion, res, start, backward));
            }
        });
    }

    @Remote
    public synchronized void cancelSearch() {
        cancelExecutions(t -> t instanceof SearchTask);
    }

    @Remote
    public synchronized void searchNext(Position start, boolean backward, int recordCount, SearchPattern pattern,
                             @Nonnull Map<String, String> hashes, long stateVersion, long requestId) {
        if (this.stateVersion > stateVersion)
            return;

        if (this.stateVersion < stateVersion)
            throw new IllegalStateException("backend_stateVersion=" + this.stateVersion + ", but UI_stateVersion=" + stateVersion);

        CompletableFuture<SearchTask.SearchResponse> execution = execute(new SearchTask(sender, logs, start, recordCount, backward, pattern, hashes, filter));

        execution.whenComplete(new LogExecutionHandler<SearchTask.SearchResponse>() {
            @Override
            protected void handle(SearchTask.SearchResponse res) {
                sender.send(new EventSearchResponse(res.getStatuses(), stateVersion, res, requestId));

                if (res.getData() != null) { // if found
                    int foundIdx = backward ? 0 : res.getData().size() - 1;
                    Record found = res.getData().get(foundIdx).getFirst();
                    assert pattern.matcher().test(found.getMessage());

                    loadNext(new Position(found, backward), backward, recordCount, hashes, stateVersion);
                }
            }
        });
    }

    private void cancelExecutions(Predicate<SessionTask<?>> filter) {
        assert Thread.holdsLock(this);

        for (Iterator<SessionTask<?>> itr = executions.iterator(); itr.hasNext();) {
            SessionTask<?> task = itr.next();
            if (filter.test(task)) {
                task.cancel();
                itr.remove();
            }
        }
    }

    private <T> CompletableFuture<T> execute(SessionTask<T> task) {
        assert Thread.holdsLock(this);

        CompletableFuture<T> future = new CompletableFuture<>();

        executions.add(task);

        task.execute((res, e) -> {
            synchronized (LogSession.this) {
                executions.remove(task);

                if (e != null) {
                    future.completeExceptionally(e);
                } else {
                    future.complete(res);
                }
            }
        });

        return future;
    }

    public synchronized void shutdown() {
        for (SessionTask<?> task : executions) {
            task.cancel();
        }

        executions.clear();

        if (logChangeNotifier != null)
            logChangeNotifier.close();
    }

    private void handleTaskError(@Nonnull Throwable e) {
        assert Thread.holdsLock(this);

        if (!(e instanceof CancellationException)) {
            LOG.error("Failed to execute session task", e);
        }
    }

    private abstract class LogExecutionHandler<T> implements BiConsumer<T, Throwable> {

        protected final long initStateVersion;

        LogExecutionHandler() {
            assert Thread.holdsLock(LogSession.this);
            initStateVersion = stateVersion;
        }

        @Override
        public void accept(T res, Throwable e) {
            try {
                synchronized (LogSession.this) {
                    if (initStateVersion != stateVersion)
                        return;

                    if (e != null) {
                        handleTaskError(e);
                    }
                    else {
                        handle(res);
                    }
                }
            } catch (Throwable e1) {
                LOG.error("Failed to handle request", e1);
            }
        }

        protected abstract void handle(T res);
    }

    private Set<LogPath> parsePathParameter(@Nullable String[] pathsFromHttpParameter) {
        if (pathsFromHttpParameter == null)
            return Collections.emptySet();

        Set<LogPath> res = new LinkedHashSet<>();

        for (String pathFromHttpParameter : pathsFromHttpParameter) {
            pathFromHttpParameter = pathFromHttpParameter.trim();

            Collection<LogPath> paths = null;

            for (LvPathResolver resolver : pathResolvers) {
                paths = resolver.resolvePath(pathFromHttpParameter);
                if (paths != null)
                    break;
            }

            if (paths == null)
                paths = LogPath.parsePathFromHttpParameter(pathFromHttpParameter);

            res.addAll(paths);
        }

        return res;
    }

    public static LogSession fromContext(@Nonnull SessionAdapter sender, @Nonnull ApplicationContext ctx) {
        LogSession res = new LogSession(sender);
        ctx.getAutowireCapableBeanFactory().autowireBeanProperties(res, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
        return res;
    }
}
