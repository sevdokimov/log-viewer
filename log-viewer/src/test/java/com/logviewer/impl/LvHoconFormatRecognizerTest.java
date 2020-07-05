package com.logviewer.impl;

import com.logviewer.TestUtils;
import com.logviewer.formats.SimpleLogFormat;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertNull;

public class LvHoconFormatRecognizerTest {

    public static final String ORIGIN_DESCRIPTION = "the-origin-description";

    @Test
    public void testEmptyFormatList() {
        LvHoconFormatRecognizer formatRecognizer = new LvHoconFormatRecognizer(ConfigFactory.parseString("{formats: []}"));
        assertNull(formatRecognizer.getFormat(Paths.get("/tmp")));
    }

    @Test
    public void testEmptyFormatProperty() {
        LvHoconFormatRecognizer formatRecognizer = new LvHoconFormatRecognizer(ConfigFactory.parseString("{formats: null}"));
        assertNull(formatRecognizer.getFormat(Paths.get("/tmp")));
    }

    @Test
    public void testDirectoryOnly() {
        String configStr = "{formats: [" +
                "{" +
                "  directory: \"/tmp/fff/\"," +
                "  format: {" +
                "      type: SimpleLogFormat" +
                "  }" +
                "}" +
                "]}";
        LvHoconFormatRecognizer formatRecognizer = new LvHoconFormatRecognizer(ConfigFactory.parseString(configStr));

        assertNull(formatRecognizer.getFormat(Paths.get("/var/bin/a.log")));
        assert formatRecognizer.getFormat(Paths.get("/tmp/fff/a.log")) instanceof SimpleLogFormat;
        assert formatRecognizer.getFormat(Paths.get("/tmp/fff/subdir/a.log")) instanceof SimpleLogFormat;
        assert formatRecognizer.getFormat(Paths.get("/tmp/fff/subdir/a.zzz")) instanceof SimpleLogFormat;
    }

    @Test
    public void testDirectoryAndRegex() {
        String configStr = "{formats: [" +
                "{" +
                "  directory: \"/tmp/fff/\"," +
                "  regex: \".+\\\\.log\"," +
                "  format: {" +
                "      type: SimpleLogFormat" +
                "  }" +
                "}" +
                "]}";
        LvHoconFormatRecognizer formatRecognizer = new LvHoconFormatRecognizer(ConfigFactory.parseString(configStr));

        assertNull(formatRecognizer.getFormat(Paths.get("/var/bin/a.log")));
        assert formatRecognizer.getFormat(Paths.get("/tmp/fff/a.log")) instanceof SimpleLogFormat;
        assert formatRecognizer.getFormat(Paths.get("/tmp/fff/subdir/a.log")) instanceof SimpleLogFormat;
        assertNull(formatRecognizer.getFormat(Paths.get("/tmp/fff/a.zzz")));
        assertNull(formatRecognizer.getFormat(Paths.get("/tmp/fff/subdir/a.zzz")));
    }

    @Test
    public void testRegexOnly() {
        String configStr = "{formats: [" +
                "{" +
                "  regex: \".+\\\\.log\"," +
                "  format: {" +
                "      type: SimpleLogFormat" +
                "  }" +
                "}" +
                "]}";
        LvHoconFormatRecognizer formatRecognizer = new LvHoconFormatRecognizer(ConfigFactory.parseString(configStr));

        assert formatRecognizer.getFormat(Paths.get("/tmp/fff/subdir/a.log")) instanceof SimpleLogFormat;
        assertNull(formatRecognizer.getFormat(Paths.get("/tmp/fff/a.zzz")));
    }

    @Test
    public void testInvalidHocon() {
        checkError("{formats: 1}", "formats");

        checkError("{formats: {}}", "formats");

        checkError("{formats: [1, 2]}", "formats");

        checkError("{formats: [{" +
                "regex: \"[\"" +
                "}]}", "Unclosed character");
    }

    private void checkError(String configText, String ... expectedText) {
        Config config = ConfigFactory.parseString(configText, ConfigParseOptions.defaults().setOriginDescription(ORIGIN_DESCRIPTION));

        ConfigException e = TestUtils.assertError(ConfigException.class, () -> new LvHoconFormatRecognizer(config));
        
        assert e.getMessage().contains(ORIGIN_DESCRIPTION) : e;

        for (String text : expectedText) {
            assert e.getMessage().contains(text) : e + " doesn't contain " + text;
        }
    }

}
