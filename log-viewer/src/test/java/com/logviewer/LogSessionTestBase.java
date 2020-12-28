package com.logviewer;

import com.logviewer.api.LvFilterPanelStateProvider;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.Filter;
import com.logviewer.data2.Log;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.utils.TestLogIdGenerator;
import com.logviewer.utils.TestPredicate;
import com.logviewer.utils.TestSessionAdapter;
import com.logviewer.web.dto.RestStatus;
import org.junit.Before;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

public class LogSessionTestBase extends AbstractLogTest {

    protected TestSessionAdapter adapter = new TestSessionAdapter();

    @Before
    public void before() {
        Log.setLogIdGenerator(TestLogIdGenerator.INSTANCE);
    }

    protected static Map<String, String> statuses(Map<String, RestStatus> map) {
        Map<String, String> res = new HashMap<>();

        for (Map.Entry<String, RestStatus> entry : map.entrySet()) {
            if (entry.getValue().getError() == null) {
                res.put(entry.getKey(), entry.getValue().getHash());
            }
        }

        return res;
    }

    @Configuration
    protected static class MultifileConfiguration extends LvTestConfig {
        @Override
        @Bean
        public LvFilterPanelStateProvider testFilterSet() {
            FilterPanelState filterPanelState = new FilterPanelState().setNamedFilters(new Filter("test-filter", true, new TestPredicate()));

            return new TestFilterPanelState().addFilterSet("default", filterPanelState);
        }

        @Bean
        public LvFormatRecognizer formatRecognizer() {
            return path -> TestUtils.MULTIFILE_LOG_FORMAT;
        }
    }
}
