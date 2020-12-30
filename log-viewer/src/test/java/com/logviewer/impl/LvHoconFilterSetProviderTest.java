package com.logviewer.impl;

import com.google.common.collect.Iterables;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.utils.LvGsonUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LvHoconFilterSetProviderTest {

    private final static String CONFIG_TEXT = "" +
            "filters = {\n" +
            "    default: {\n" +
            "        exceptionsOnly: true\n" +

            "        groovyFilters: [\n" +
            "          {\n" +
            "              \"name\": \"Errors only\",\n" +
            "              \"id\": f1,\n" +
            "              \"script\": \"level !\\u003d \\u0027ERROR\\u0027\"" +
            "          },\n" +
            "        ]\n" +
            "    }" +
            "}";

    @Test
    public void test() {
        Config config = ConfigFactory.parseString(CONFIG_TEXT).resolve();

        LvHoconFilterPanelStateProvider provider = new LvHoconFilterPanelStateProvider(config);

        Map<String, String> filterSets = provider.getFilterSets();

        assertEquals("default", Iterables.getOnlyElement(filterSets.keySet()));

        FilterPanelState state = LvGsonUtils.GSON.fromJson(Iterables.getOnlyElement(filterSets.values()), FilterPanelState.class);

        assertEquals(1, state.getGroovyFilters().length);

        assertEquals("Errors only", state.getGroovyFilters()[0].getName());

        assert state.getExceptionsOnly();
    }

}
