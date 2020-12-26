package com.logviewer;

import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.*;
import com.logviewer.filters.CompositeRecordPredicate;
import com.logviewer.filters.DatePredicate;
import com.logviewer.filters.FieldArgPredicate;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.Pair;
import com.logviewer.utils.TestPredicate;
import com.logviewer.utils.Utils;
import com.logviewer.web.session.LogDataListener;
import com.logviewer.web.session.LogProcess;
import com.logviewer.web.session.SearchResult;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.SearchPattern;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TimeRangeFromFilterTest extends AbstractLogTest {

    @Test
    public void testSameStartAndEndDate() throws InterruptedException {
        Log log = getLog();

        RecordPredicate filter = CompositeRecordPredicate.and(new TestPredicate(), new DatePredicate(date("150101 10:00:05"), true),
                new DatePredicate(date("150101 10:00:05"), false));

        ResultListener res = process(r -> log.loadRecords(filter, 100, null, true, null, 1000000, r));

        assertEquals(Arrays.asList("555 3", "555 2", "555 1"), res.records.stream().map(r -> r.getFieldText(1)).collect(Collectors.toList()));
        assert res.eof;
        assertEquals(TestPredicate.getPassed().size(), res.records.size());

        ResultListener resForwardFrom0 = process(r -> {
            return log.loadRecords(filter, 100, new Position(log.getId(), 0, 0), false, null, 1000000, r);
        });

        assertEquals(res, resForwardFrom0);
        assertEquals(TestPredicate.getPassed().size(), res.records.size());

        ResultListener resForwardFrom5 = process(r -> {
            return log.loadRecords(filter, 100, new Position(log.getId(), 0, recordIndex(log, "150101 10:00:05 555 1")), false, null, 1000000, r);
        });

        assertEquals(res, resForwardFrom5);
        assertEquals(TestPredicate.getPassed().size(), res.records.size());

        ResultListener resBackwardFrom6 = process(r -> {
            return log.loadRecords(filter, 100, new Position(log.getId(), 0, recordIndex(log, "150101 10:00:06 666 3")), true, null, 1000000, r);
        });

        assertEquals(res, resBackwardFrom6);
        assertEquals(TestPredicate.getPassed().size(), res.records.size());

        ResultListener resForeignLog = process(r -> {
            return log.loadRecords(filter, 100, new Position("zzz", date("150101 10:00:02"), 0), false, null, 1000000, r);
        });

        assertEquals(res, resForeignLog);
        assertEquals(TestPredicate.getPassed().size(), res.records.size());

        ResultListener resForeignLogBack = process(r -> {
            return log.loadRecords(filter, 100, new Position("zzz", date("150101 10:00:08"), 0), true, null, 1000000, r);
        });

        assertEquals(res, resForeignLogBack);
        assertEquals(TestPredicate.getPassed().size(), res.records.size());
    }

    @Test
    public void testSearch() throws InterruptedException {
        Log log = getLog();

        RecordPredicate filter = CompositeRecordPredicate.and(new TestPredicate(), new DatePredicate(date("150101 10:00:05"), true),
                new DatePredicate(date("150101 10:00:08"), false), new FieldArgPredicate("msg", "x"));

        Position start = new Position(log.getId(), 0, recordIndex(log, "150101 10:00:06 666 2"));

        SearchConsumer res = new SearchConsumer();
        LogProcess process = log.createRecordSearcher(start, false, filter, null, 1000, new SearchPattern(".+", false, true), res);
        process.start();
        res.resultWaiter.await();

        assert !res.res.isFound();
        assertEquals(Arrays.asList("666 2", "666 3", "777 1", "777 2", "777 3", "888 1", "888 2", "888 3"),
                TestPredicate.getPassed().stream().map(r -> r.getFieldText(1)).collect(Collectors.toList()));

        TestPredicate.clear();

        res = new SearchConsumer();
        process = log.createRecordSearcher(start, true, filter, null, 1000, new SearchPattern(".+", false, true), res);
        process.start();
        res.resultWaiter.await();

        assert !res.res.isFound();
        assertEquals(Arrays.asList("666 1", "555 3", "555 2", "555 1"),
                TestPredicate.getPassed().stream().map(r -> r.getFieldText(1)).collect(Collectors.toList()));

        TestPredicate.clear();
        res = new SearchConsumer();
        process = log.createRecordSearcher(new Position("zzz", date("150101 10:00:06"), 0), true, filter, null, 1000, new SearchPattern(".+", false, true), res);
        process.start();
        res.resultWaiter.await();

        assert !res.res.isFound();
        assertEquals(Arrays.asList("666 3", "666 2", "666 1", "555 3", "555 2", "555 1"),
                TestPredicate.getPassed().stream().map(r -> r.getFieldText(1)).collect(Collectors.toList()));

        TestPredicate.clear();
        res = new SearchConsumer();
        process = log.createRecordSearcher(new Position("zzz", date("150101 10:00:06"), 0), false, filter, null, 1000, new SearchPattern(".+", false, true), res);
        process.start();
        res.resultWaiter.await();

        assert !res.res.isFound();
        assertEquals(Arrays.asList("777 1", "777 2", "777 3", "888 1", "888 2", "888 3"),
                TestPredicate.getPassed().stream().map(r -> r.getFieldText(1)).collect(Collectors.toList()));
    }

    private ResultListener process(Function<ResultListener, LogProcess> processFactory) throws InterruptedException {
        ResultListener res = new ResultListener();
        TestPredicate.clear();

        LogProcess process = processFactory.apply(res);
        process.start();
        res.waitForFinish();

        return res;
    }

    @Nonnull
    private Log getLog() {
        ApplicationContext ctx = createContext(LvTestConfig.class);
        return ctx.getBean(LogService.class).openLog(getTestLog("test-log.log"), TestUtils.MULTIFILE_LOG_FORMAT);
    }

    private static long date(String date) {
        try {
            return new SimpleDateFormat("yyMMdd HH:mm:ss").parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SearchConsumer implements Consumer<SearchResult> {
        private SearchResult res;

        private final CountDownLatch resultWaiter = new CountDownLatch(1);

        @Override
        public void accept(SearchResult searchResult) {
            assert res == null;
            res = searchResult;

            resultWaiter.countDown();
        }
    }

    private static class ResultListener implements LogDataListener {

        private final List<Record> records = new ArrayList<>();
        private final LinkedList<Record> reversedRecords = new LinkedList<>();

        private boolean eof;

        private final CountDownLatch finishWaiter = new CountDownLatch(1);

        @Override
        public void onData(RecordList data) {
            for (Pair<Record, Throwable> pair : data) {
                if (pair.getSecond() != null) {
                    throw Utils.propagate(pair.getSecond());
                }

                records.add(pair.getFirst());
                reversedRecords.addFirst(pair.getFirst());
            }
        }

        @Override
        public void onFinish(Status status, boolean eof) {
            if (status.getError() != null)
                throw Utils.propagate(status.getError());

            this.eof = eof;

            assert finishWaiter.getCount() == 1;
            finishWaiter.countDown();
        }

        public void waitForFinish() throws InterruptedException {
            finishWaiter.await();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResultListener that = (ResultListener) o;
            return eof == that.eof && (records.equals(that.records) || reversedRecords.equals(that.records));
        }

        @Override
        public int hashCode() {
            return Objects.hash(records, eof);
        }
    }
}
