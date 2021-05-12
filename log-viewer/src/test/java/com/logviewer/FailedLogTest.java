package com.logviewer;

import static com.logviewer.utils.TestSessionAdapter.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.logviewer.data2.Position;
import com.logviewer.web.dto.LogList;
import com.logviewer.web.dto.events.EventNextDataLoaded;
import com.logviewer.web.dto.events.EventScrollToEdgeResponse;
import com.logviewer.web.dto.events.EventSearchResponse;
import com.logviewer.web.session.LogSession;
import com.logviewer.web.session.tasks.SearchPattern;

public class FailedLogTest extends LogSessionTestBase {

    @Test
    public void workWithBrokenLog() throws Exception {
        ApplicationContext ctx = createContext(MultifileConfiguration.class);

        LogSession session = LogSession.fromContext(adapter, ctx);

        session.init(LogList.of(getTestLog("log.log"), "/unexisting-log.log"), null, null);
        session.scrollToEdge(3, 2, null, false);

        EventScrollToEdgeResponse resp = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, String> hashes = statuses(resp.statuses);

        assertNull(resp.statuses.get("log.log").getErrorType());
        assertNotNull(resp.statuses.get("unexisting-log.log").getErrorType());

        session.searchNext(new Position("zzz.log", TestUtils.date(0, 2), 0), false, 1, new SearchPattern("f"), hashes,
            2, 1, false);

        adapter.check(EventSearchResponse.class, stateVersion(2), event -> {
            TestUtils.check(event.records, "150101 10:00:05 f");
            assert event.hasSkippedLine;
            assertEquals(1L, event.requestId);
        });

        session.loadNext(new Position("zzz.log", TestUtils.date(0, 2), 0), false, 2, hashes, 2);

        adapter.check(EventNextDataLoaded.class, stateVersion(2), records("150101 10:00:03 c", "150101 10:00:03 d"));
    }

}
