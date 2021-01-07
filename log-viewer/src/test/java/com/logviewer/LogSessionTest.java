package com.logviewer;

import com.logviewer.data2.LogCrashedException;
import com.logviewer.data2.Position;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.TestPredicate;
import com.logviewer.web.dto.LogList;
import com.logviewer.web.dto.events.*;
import com.logviewer.web.session.LogSession;
import com.logviewer.web.session.tasks.SearchPattern;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.logviewer.utils.TestSessionAdapter.*;
import static org.junit.Assert.assertEquals;

public class LogSessionTest extends LogSessionTestBase {

//    protected static EditableConfig testConfig;
//    protected static LogService testLogService;
//
//    static {
//        testConfig = new EditableConfig();
//        testLogService = new LogService(testConfig);
//    }
//
//    @BeforeClass
//    public static void beforeClass() throws Exception {
//        if (LogContext.getLogService() != testLogService) {
//            LogContext.init(testLogService, new InmemoryFavoritesService());
//        }
//    }
//

    @Test
    public void testScrollDown() throws InterruptedException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(getTestLog("multilog/server-a.log"), getTestLog("multilog/server-b.log")), null, null);
        session.scrollToEdge(3, 2, null, false);

        adapter.skipAndCheck(EventScrollToEdgeResponse.class, noError(), stateVersion(2), recordsSorted(),
                field(session, "msg", "a 4", "b 4", "b 5"), hasNext());

        session.scrollToEdge(6, 3, null, false);

        adapter.skipAndCheck(EventScrollToEdgeResponse.class, noError(), stateVersion(3), recordsSorted(),
                field(session, "msg", "b 2", "b 3", "a 3", "a 4", "b 4", "b 5"), hasNext());

        session.scrollToEdge(100, 4, null, false);

        adapter.skipAndCheck(EventScrollToEdgeResponse.class, noError(), stateVersion(4), recordsSorted(),
                field(session, "msg", "a 1", "a 2", "b 1", "b 2", "b 3", "a 3", "a 4", "b 4", "b 5"), hasNext(false));
    }

    @Test
    public void testLoadNext() throws InterruptedException, IOException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/multilog.log"))), "default", null);
        session.scrollToEdge(3, 2, new RecordPredicate[]{new TestPredicate()}, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(init.statuses);

        // Load A first
        Object lock = TestPredicate.lock(Pattern.compile(".*b"));

        session.loadNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 3, hashes, 2);

        TestPredicate.waitForRecord("150101 10:00:05 a");
        TestPredicate.unlock(lock);

        adapter.check(EventNextDataLoaded.class, (Consumer<StatusHolderEvent>) event -> {
            assertEquals(hashes, statuses(event.statuses));
        }, stateVersion(2), records("150101 10:00:01 a", "150101 10:00:01 b", "150101 10:00:03 a"));

        // Load B first
        TestPredicate.clear();
        lock = TestPredicate.lock(Pattern.compile(".*a"));

        session.loadNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 3, hashes, 2);

        TestPredicate.waitForRecord("150101 10:00:04 b");
        TestPredicate.unlock(lock);

        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records("150101 10:00:01 a", "150101 10:00:01 b", "150101 10:00:03 a"), hasNext());

        TestPredicate.clear();

        //
        session.loadNext(new Position("b.log", TestUtils.date(0, 1), 0), false, 3, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records("150101 10:00:01 b", "150101 10:00:03 a", "150101 10:00:03 b"), hasNext());

        //
        session.loadNext(new Position("c.log", TestUtils.date(0, 2), 0), false, 3, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records("150101 10:00:03 a", "150101 10:00:03 b", "150101 10:00:04 b"), hasNext());

        //
        session.loadNext(new Position("c.log", TestUtils.date(0, 2), 0), false, 100, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records("150101 10:00:03 a", "150101 10:00:03 b", "150101 10:00:04 b", "150101 10:00:05 a"), hasNext(false));

        //
        session.loadNext(new Position("c.log", TestUtils.date(10, 0), 0), false, 100, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records(), hasNext(false));

        // ---=== Back ===---
        session.loadNext(new Position("c.log", TestUtils.date(0, 10), 0), true, 3, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records(true, "150101 10:00:03 b", "150101 10:00:04 b", "150101 10:00:05 a"));

        session.loadNext(new Position("a.log", TestUtils.date(0, 3), 18), true, 3, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records(false, "150101 10:00:01 a", "150101 10:00:01 b"));

        session.loadNext(new Position("b.log", TestUtils.date(0, 3), 18), true, 3, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records(false, "150101 10:00:01 a", "150101 10:00:01 b", "150101 10:00:03 a"));

        session.loadNext(new Position("b.log", TestUtils.date(0, 4), 18 * 2), true, 3, hashes, 2);
        adapter.check(EventNextDataLoaded.class, stateVersion(2),
                records(true, "150101 10:00:01 b", "150101 10:00:03 a", "150101 10:00:03 b"));

        lock = TestPredicate.lock("150101 10:00:01 b");

        session.loadNext(new Position("b.log", TestUtils.date(0, 3), 18), true, 2, hashes, 2);

        TestPredicate.waitForLocked("150101 10:00:01 b");
        TestPredicate.unlock(lock);

        adapter.check(EventNextDataLoaded.class, stateVersion(2), records(true, "150101 10:00:01 b", "150101 10:00:03 a"));
    }

    @Test
    public void testSearch() throws InterruptedException, IOException, LogCrashedException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/search.log"))), "default", null);
        session.scrollToEdge(3, 2, new RecordPredicate[]{new TestPredicate()}, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(init.statuses);

        //
        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("qssxcgr"), hashes, 2, 1);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult(), reqId(1));

        //
        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 3, new SearchPattern("xxx"), hashes, 2, 2);
        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(2),
                searchResult(false, "150101 10:00:01 zzz a", "150101 10:00:01 xxx b"));
        adapter.waitForType(EventNextDataLoaded.class);

        //
        TestPredicate.clear();
        Object lock = TestPredicate.lock("150101 10:00:03 a");
        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("00:04 b"), hashes, 2, 3);
        TestPredicate.waitForRecord("150101 10:00:04 b");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(3),
                searchResult(true, "150101 10:00:03 a", "150101 10:00:04 b"));
        adapter.waitForType(EventNextDataLoaded.class);

        //
        lock = TestPredicate.lock("150101 10:00:04 b");

        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("00:04 b"), hashes, 2, 4);
        TestPredicate.waitForRecord("150101 10:00:06 a");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(4),
                searchResult(true, "150101 10:00:03 a", "150101 10:00:04 b"));
        adapter.waitForType(EventNextDataLoaded.class);

        // Backward
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 2, new SearchPattern("qssxcgr"), hashes, 2, 5);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult());

        //
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 2, new SearchPattern("xxx"), hashes, 2, 6);
        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a"));
        adapter.waitForType(EventNextDataLoaded.class);

        //
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 3, new SearchPattern("xxx"), hashes, 2, 6);
        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a", "150101 10:00:03 a"));
        adapter.waitForType(EventNextDataLoaded.class);

        //
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 4, new SearchPattern("xxx"), hashes, 2, 7);
        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a", "150101 10:00:03 a", "150101 10:00:04 b"));
        adapter.waitForType(EventNextDataLoaded.class);

        //
        TestPredicate.clear();
        lock = TestPredicate.lock("150101 10:00:04 b");
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 4, new SearchPattern("150101 10:00:04 b"), hashes, 2, 8);
        TestPredicate.waitForRecord("150101 10:00:01 zzz a");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(false, "150101 10:00:04 b", "150101 10:00:05 a", "150101 10:00:06 a"));
    }

    @Test
    public void test2tailLopader() throws InterruptedException, IOException, LogCrashedException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/search3.log"))), "default", null);
        session.scrollToEdge(3, 2, new RecordPredicate[]{new TestPredicate()}, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(init.statuses);

        assertEquals(3, hashes.size());

        Object lock = TestPredicate.lock("150101 10:00:06 ccc c");

        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("10:00:06 ccc"), hashes, 2, 2);

        TestPredicate.waitForRecord("150101 10:00:09 a");
        TestPredicate.waitForRecord("150101 10:00:09 b");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(2),
                searchResult(true, "150101 10:00:06 a", "150101 10:00:06 ccc c"));
    }

    @Test
    public void testCanceling() throws InterruptedException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(getTestLog("multilog/server-a.log")), "default", null);
        session.scrollToEdge(3, 2, new RecordPredicate[]{new TestPredicate()}, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(init.statuses);

        Object lock = TestPredicate.lock("150101 10:00:01 a 1");

        session.loadNext(new Position("server-a.log", TestUtils.date(0, 1), 0), false, 4, hashes, 2);

        TestPredicate.waitForLocked("150101 10:00:01 a 1");

        session.scrollToEdge(2, 3, new RecordPredicate[]{new TestPredicate()}, false);
        EventScrollToEdgeResponse e2 = adapter.waitForType(EventScrollToEdgeResponse.class);
        assertEquals(3, e2.stateVersion);

        TestPredicate.unlock(lock);
    }

    @Test
    public void testNoSystemPropertiesOnUI() throws InterruptedException, IOException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/multilog.log"))), "default", null);

        EventSetViewState init = adapter.waitForType(EventSetViewState.class);

        Config uiConfig = ConfigFactory.parseString(init.getUiConfig());

        assert !uiConfig.hasPath("java.class.path");
    }

    public String[] createMultifileLog(String file) throws IOException {
        return TestUtils.createMultifileLog(file);
    }
}