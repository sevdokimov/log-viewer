package com.logviewer.impl;

import com.google.common.collect.Iterables;
import com.logviewer.domain.FilterPanelState;
import com.logviewer.filters.GroovyPredicate;
import com.logviewer.utils.LvGsonUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class LvHoconFilterSetProviderTest {

    private final static String CONFIG_TEXT = "" +
            "filters = {\n" +
            "    default: {\n" +
            "        namedFilters: [\n" +
            "        {\n" +
            "            \"name\": \"Errors only\",\n" +
            "            \"enabled\": false,\n" +
            "            \"predicate\": {\n" +
            "                \"type\": \"GroovyPredicate\",\n" +
            "                \"script\": \"level !\\u003d \\u0027ERROR\\u0027\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"enabled\": true,\n" +
            "            \"predicate\": {\n" +
            "                \"type\": \"GroovyPredicate\",\n" +
            "                \"script\": \"level !\\u003d \\u0027ERROR\\u0027 \\u0026\\u0026 level !\\u003d \\u0027WARN\\u0027\"\n" +
            "            }\n" +
            "        }\n" +
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

        assertEquals(2, state.getNamedFilters().length);

        assertEquals("Errors only", state.getNamedFilters()[0].getName());
        assertFalse(state.getNamedFilters()[0].isEnabled());
        assertTrue(state.getNamedFilters()[0].getPredicate() instanceof GroovyPredicate);

        assertNull(state.getNamedFilters()[1].getName());
        assertTrue(state.getNamedFilters()[1].isEnabled());
        assertTrue(state.getNamedFilters()[1].getPredicate() instanceof GroovyPredicate);
    }

}
