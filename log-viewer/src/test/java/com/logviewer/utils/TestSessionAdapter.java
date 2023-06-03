package com.logviewer.utils;

import com.logviewer.TestUtils;
import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.dto.RestStatus;
import com.logviewer.web.dto.events.BackendEvent;
import com.logviewer.web.dto.events.DataHolderEvent;
import com.logviewer.web.dto.events.EventSearchResponse;
import com.logviewer.web.dto.events.StatusHolderEvent;
import com.logviewer.web.session.SessionAdapter;
import com.logviewer.web.session.Status;
import org.junit.Assert;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNull;

public class TestSessionAdapter implements SessionAdapter {

    public static final Object ANY = new Object();

    private BlockingDeque<BackendEvent> events = new LinkedBlockingDeque<>();

    @Override
    public void send(@NonNull BackendEvent event) {
        events.add(event);
    }

    @SafeVarargs
    public final <T extends BackendEvent> void check(Class<T> type, Consumer<? super T>... tests) throws InterruptedException {
        T event = nextEvent(type);
        checkEvent(event, tests);
    }

    @SafeVarargs
    public final <T extends BackendEvent> void skipAndCheck(Class<T> type, Consumer<? super T>... tests) throws InterruptedException {
        T event = waitForType(type);
        checkEvent(event, tests);
    }

    public <T extends BackendEvent> T waitForType(Class<T> type) throws InterruptedException {
        BackendEvent event;

        do {
            event = events.poll(TestUtils.WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
            if (event == null)
                throw new RuntimeException("Timeout");
        }
        while (!type.isInstance(event));

        return (T) event;
    }

    public <T extends BackendEvent> T nextEvent(Class<T> type) throws InterruptedException {
        BackendEvent event = events.take();

        assert type.isInstance(event) : "Unexpected event: " + event.getName() + " (should be " + type.getSimpleName() + ")";

        return (T) event;
    }

    private <T extends BackendEvent> void checkEvent(T event, Consumer<? super T>... tests) {
        for (Consumer<? super T> test : tests) {
            test.accept(event);
        }
    }

//    private void checkArguments(Object[] expected, Object[] actual) {
//        Assert.assertEquals(expected.length, actual.length);
//
//        for (int i = 0; i < expected.length; i++) {
//            if (expected[i] == ANY)
//                continue;
//
//            if (expected[i] instanceof Consumer) {
//                ((Consumer) expected[i]).accept(actual[i]);
//            }
//            else {
//                assert Objects.deepEquals(expected[i], actual[i]): "[" + i + "] " + expected[i] + " != " + actual[i];
//            }
//        }
//    }

    public static Consumer<StatusHolderEvent> stateVersion(long stateVersion) {
        return event -> {
            assert event.stateVersion == stateVersion : "Incorrect state version [expected=" + stateVersion
                    + ", actual=" + event.stateVersion;
        };
    }

    public static Consumer<DataHolderEvent> field(String filedName, String ... expectedValues) {
        return event -> {
            List<String> actualValues = event.data.records.stream().map(r -> r.fieldValue(filedName)).collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList(expectedValues), actualValues);
        };
    }

    public static Consumer<DataHolderEvent> records(String ... expectedRecords) {
        return event -> TestUtils.check(event.data.records, expectedRecords);
    }

    public static Consumer<EventSearchResponse> searchResult(Boolean hasSkippedLines, String ... expectedRecords) {
        return event -> {
            if (hasSkippedLines != null)
                Assert.assertEquals(hasSkippedLines, event.hasSkippedLine);

            if (expectedRecords.length == 0) {
                assertNull(event.records);
            }
            else {
                TestUtils.check(event.records, expectedRecords);
            }
        };
    }

    public static Consumer<EventSearchResponse> searchResult(String ... expectedRecords) {
        return searchResult(null, expectedRecords);
    }

    public static Consumer<EventSearchResponse> reqId(long reqId) {
        return event -> {
            assert event.requestId == reqId;
        };
    }

    public static Consumer<StatusHolderEvent> noError() {
        return event -> {
            for (RestStatus status : event.statuses.values()) {
                assert status.getErrorType() == null;
            }
        };
    }

    public static Consumer<DataHolderEvent> recordsSorted() {
        return event -> TestUtils.checkOrder(event.data.records, Comparator.comparing(RestRecord::getText));
    }

    public static Consumer<StatusHolderEvent> hasNext() {
        return hasNext(true);
    }

    public static Consumer<StatusHolderEvent> hasNext(boolean hasNext) {
        return event -> {
            assertHasNextLine(event, hasNext);
        };
    }

    public static void assertHasNextLine(StatusHolderEvent event, boolean hasNext) {
        if (hasNext) {
            assert event.statuses.values().stream().anyMatch(s -> (s.getFlags() & Status.FLAG_EOF) == 0);
        } else {
            assert event.statuses.values().stream().allMatch(s -> (s.getFlags() & Status.FLAG_EOF) != 0);
        }
    }

    public static Consumer<DataHolderEvent> records(boolean hasNextLine, String ... expectedRecords) {
        return event -> {
            TestUtils.check(event.data.records, expectedRecords);
            assertHasNextLine(event, hasNextLine);
        };
    }
}
