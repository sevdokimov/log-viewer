package com.logviewer.impl;

import com.logviewer.TestUtils;
import com.logviewer.data2.LogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class LvPatternFormatRecognizerTest {

    @Test
    public void testFormatDetector() {
        Config config = ConfigFactory.parseString("" +
                "logs = [" +
                "{\n" +
                "  path: \"/aaa/bbb/**/*.log\"\n" +
                "  format: {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]");

        LvPatternFormatRecognizer manager = new LvPatternFormatRecognizer(LvPatternFormatRecognizer.fromHocon(config));

        LogFormat format = manager.getFormat(Paths.get("/aaa/bbb/l.log"));
        assertThat(format, instanceOf(LogbackLogFormat.class));

        assertThat(((LogbackLogFormat)format).getPattern(), CoreMatchers.is("%date [%thread] %level %logger - %msg%n"));

        assertThat(manager.getFormat(Paths.get("/")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa.log")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbb/aaa.out")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbb")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbbbb/lll.log")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaaaa/bbb/lll.log")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbb/lll.logg")), nullValue());

        assertThat(manager.getFormat(Paths.get("/aaa/bbb/lll.log")), notNullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbb/.log")), notNullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbb/zzz/lll.log")), notNullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/bbb/zzz/xxx/lll.log")), notNullValue());
    }

    @Test
    public void testFormatDetector2() {
        Config config = ConfigFactory.parseString("" +
                "logs = [\n" +
                "{" +
                "  path: \"**/*.log\"\n" +
                "},\n" +
                "{" +
                "  path:\"/aaa/**/aaa.log\"\n" +
                "  format: {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]");

        LvPatternFormatRecognizer manager = new LvPatternFormatRecognizer(LvPatternFormatRecognizer.fromHocon(config));

        assertThat(manager.getFormat(Paths.get("/sss.log")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/wwww/aaa.log")), notNullValue());
    }

    @Test
    public void testInvalidFormat1() {
        Config config = ConfigFactory.parseString("" +
                "logs = [\n" +
                "{\n" +
                "  path: \"**/*.log\"\n" +
                "},\n" +
                "{\n" +
                "  path:\"/aaa/**/aaa.log\"\n" +
                "  format: {pattern: \"%date [%thread] %level %logger - %msg%n\"}\n" +
                "}\n" +
                "]");

        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> LvPatternFormatRecognizer.fromHocon(config));
        assertTrue(e.getMessage().contains("line=7"));
    }

    @Test
    public void testInvalidFormat2() {
        Config config = ConfigFactory.parseString("" +
                "logs = [\n" +
                "{\n" +
                "  path: \"**/*.log\"\n" +
                "},\n" +
                "{\n" +
                "  path:\"/aaa/**/aaa.log\"\n" +
                "  format: {type: \"LogbackLogFormat\"}\n" +
                "}\n" +
                "]");


        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> LvPatternFormatRecognizer.fromHocon(config));
        assertTrue(e.getMessage().contains("line=7"));
    }

    @Test
    public void testInvalidFormat3() {
        Config config = ConfigFactory.parseString("" +
                "logs = [\n" +
                "{\n" +
                "  path: \"**/*.log\"\n" +
                "},\n" +
                "{\n" +
                "  path:\"/aaa/**/aaa.log\"\n" +
                "  format: {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n%\"}\n" +
                "}\n" +
                "]");


        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> LvPatternFormatRecognizer.fromHocon(config));
        assertTrue(e.getMessage().contains("line=7"));
    }

}
