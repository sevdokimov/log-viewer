package com.logviewer;

import com.logviewer.data2.ParserConfig;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        session.init(LogList.of(getTestLog("multilog/server-a.log"), getTestLog("multilog/server-b.log")));
        session.scrollToEdge(3, 2, null, false);

        adapter.skipAndCheck(EventScrollToEdgeResponse.class, noError(), stateVersion(2), recordsSorted(),
                field("msg", "a 4", "b 4", "b 5"), hasNext());

        session.scrollToEdge(6, 3, null, false);

        adapter.skipAndCheck(EventScrollToEdgeResponse.class, noError(), stateVersion(3), recordsSorted(),
                field("msg", "b 2", "b 3", "a 3", "a 4", "b 4", "b 5"), hasNext());

        session.scrollToEdge(100, 4, null, false);

        adapter.skipAndCheck(EventScrollToEdgeResponse.class, noError(), stateVersion(4), recordsSorted(),
                field("msg", "a 1", "a 2", "b 1", "b 2", "b 3", "a 3", "a 4", "b 4", "b 5"), hasNext(false));
    }

    @Test
    public void testLoadNext() throws InterruptedException, IOException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/multilog.log"))));
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
    public void testSearch() throws InterruptedException, IOException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/search.log"))));
        session.scrollToEdge(3, 2, new RecordPredicate[]{new TestPredicate()}, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(init.statuses);

        //
        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("qssxcgr"), hashes, 2, 1, false);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult(), reqId(1));

        //
        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 3, new SearchPattern("xxx"), hashes, 2, 2, false);
        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(2), searchResult(false, "150101 10:00:01 zzz a", "150101 10:00:01 xxx b"));

        //
        TestPredicate.clear();
        Object lock = TestPredicate.lock("150101 10:00:03 a");
        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("00:04 b"), hashes, 2, 3, false);
        TestPredicate.waitForRecord("150101 10:00:04 b");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(3), searchResult(true, "150101 10:00:03 a", "150101 10:00:04 b"));

        //
        lock = TestPredicate.lock("150101 10:00:04 b");

        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("00:04 b"), hashes, 2, 4, false);
        TestPredicate.waitForRecord("150101 10:00:06 a");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(4), searchResult(true, "150101 10:00:03 a", "150101 10:00:04 b"),
                res -> assertEquals(1, res.foundIdx));

        // Backward
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 2, new SearchPattern("qssxcgr"), hashes, 2, 5, false);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult());

        //
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 2, new SearchPattern("xxx"), hashes, 2, 6, false);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a"));

        //
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 3, new SearchPattern("xxx"), hashes, 2, 6, false);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a", "150101 10:00:03 a"));

        //
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 4, new SearchPattern("xxx"), hashes, 2, 7, false);
        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a", "150101 10:00:03 a",
            "150101 10:00:04 b"));

        //
        TestPredicate.clear();
        lock = TestPredicate.lock("150101 10:00:04 b");
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 4, new SearchPattern("150101 10:00:04 b"), hashes, 2, 8, false);
        TestPredicate.waitForRecord("150101 10:00:01 zzz a");
        TestPredicate.unlock(lock);

        adapter.check(EventSearchResponse.class, stateVersion(2), searchResult(false,"150101 10:00:04 b", "150101 10:00:05 a", "150101 10:00:06 a"));

        // load records after found line
        session.searchNext(new Position("z.log", TestUtils.date(0, 1), 0), false, 4, new SearchPattern("10:00:04 b"), hashes, 2, 8, true);
        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(true, "150101 10:00:02 xxx a", "150101 10:00:03 a", "150101 10:00:03 a", "150101 10:00:04 b",
                "150101 10:00:05 a", "150101 10:00:06 a"),
                res -> assertEquals(3, res.foundIdx),
                hasNext(false));

        // load records after found line (backward)
        session.searchNext(new Position("z.log", TestUtils.date(0, 10), 0), true, 4, new SearchPattern("10:00:04 b"), hashes, 2, 9, true);
        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(false, "150101 10:00:01 xxx b", "150101 10:00:02 xxx a", "150101 10:00:03 a", "150101 10:00:03 a", "150101 10:00:04 b",
                        "150101 10:00:05 a", "150101 10:00:06 a"),
                hasNext(true),
                res -> assertEquals(4, res.foundIdx));

        // load records after found line (waiting)
        TestPredicate.clear();
        lock = TestPredicate.lock("150101 10:00:05 a");

        session.searchNext(new Position("z.log", TestUtils.date(0, 1), 0), false, 5, new SearchPattern("150101 10:00:03 a"), hashes, 2, 8, true);
        adapter.check(EventSearchResponse.class, stateVersion(2),
                searchResult(false, "150101 10:00:02 xxx a", "150101 10:00:03 a"),
                hasNext(true),
                res -> assertEquals(1, res.foundIdx));

        TestPredicate.unlock(lock);

        adapter.check(EventNextDataLoaded.class, resp -> {
            TestUtils.check(resp.data.records, "150101 10:00:03 a", "150101 10:00:04 b", "150101 10:00:05 a", "150101 10:00:06 a");
            assertHasNextLine(resp, false);
        });
    }

    @Test
    public void test2tailLopader() throws InterruptedException, IOException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/search3.log"))));
        session.scrollToEdge(3, 2, new RecordPredicate[]{new TestPredicate()}, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(init.statuses);

        assertEquals(3, hashes.size());

        session.searchNext(new Position("a.log", TestUtils.date(0, 1), 0), false, 2, new SearchPattern("10:00:06 ccc"), hashes, 2, 2, false);

        adapter.check(EventSearchResponse.class, stateVersion(2), reqId(2), searchResult(true, "150101 10:00:06 a", "150101 10:00:06 ccc c"));
    }

    @Test
    public void testCanceling() throws InterruptedException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(getTestLog("multilog/server-a.log")));
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

        session.init(LogList.of(createMultifileLog(getTestLog("multilog/multilog.log"))));

        EventSetViewState init = adapter.waitForType(EventSetViewState.class);

        Config uiConfig = ConfigFactory.parseString(init.getUiConfig());

        assert !uiConfig.hasPath("java.class.path");
    }

    @Test
    public void testLoadContent() throws IOException, InterruptedException {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);
        LogSession session = LogSession.fromContext(adapter, ctx);

        String file = getTestLog("big-log-event.log");

        session.init(LogList.of(file));

        int offset = 100;

        session.loadLogContent(session.getLogs()[0].getId(), 777, offset, offset + ParserConfig.MAX_LINE_LENGTH + 1000);

        LoadLogContentResponse resp = adapter.waitForType(LoadLogContentResponse.class);

        assertEquals(offset, resp.getOffset());
        assertEquals(777, resp.getRecordStart());

        int loadedBytes = resp.getTextLengthBytes();
        assert loadedBytes <= ParserConfig.MAX_LINE_LENGTH && loadedBytes > ParserConfig.MAX_LINE_LENGTH - 20;

        byte[] bytes = Files.readAllBytes(Paths.get(session.getLogs()[0].getPath().getFile()));
        assertEquals(new String(bytes, offset, loadedBytes, StandardCharsets.UTF_8), resp.getText());
    }

    public String[] createMultifileLog(String file) throws IOException {
        return TestUtils.createMultifileLog(file);
    }
}