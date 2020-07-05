package com.logviewer;

import com.logviewer.data2.LogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class LogManagerTest {

    @Test
    public void hoconParsing() {
        List<LogDescriptor> descriptors = LogManager.fromHocon(ConfigFactory.parseString("" +
                "logs =[" +
                "{\n" +
                "  path: \"/aaa/**/*.log\"\n" +
                "}\n" +

                "{ " +
                "  path: \"**/*.out\"\n" +
                "  format: {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}\n" +
                "}" +
                "]"));

        assertEquals(2, descriptors.size());

        descriptors = descriptors.stream().sorted(Comparator.comparing(d -> d.getFilePattern().pattern())).collect(Collectors.toList());
        
        LogDescriptor d1 = descriptors.get(0);
        assertEquals(".*[/\\\\]?[^/\\\\]*\\.out", d1.getFilePattern().pattern());
        assertEquals(".*", d1.getSubdirPatterns().stream().map(Pattern::pattern).collect(Collectors.joining(" , ")));
        LogFormat format = d1.getFormat();
        assertTrue(format instanceof LogbackLogFormat);

        LogDescriptor d2 = descriptors.get(1);
        assertEquals("[\\\\/]aaa[\\\\/].*[/\\\\]?[^/\\\\]*\\.log", d2.getFilePattern().pattern());
        assertEquals("/ , [\\\\/]aaa , [\\\\/]aaa[\\\\/].*", d2.getSubdirPatterns().stream().map(Pattern::pattern).collect(Collectors.joining(" , ")));
        assertNull(d2.getFormat());
    }

    @Test
    public void testFormatDetector() {
        Config config = ConfigFactory.parseString("" +
                "logs = [" +
                "{\n" +
                "  path: \"/aaa/bbb/**/*.log\"\n" +
                "  format: {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]");

        List<LogDescriptor> descriptors = LogManager.fromHocon(config);
        LogManager manager = new LogManager(descriptors);

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
                "}" +
                "{" +
                "  path:\"/aaa/**/aaa.log\"\n" +
                "  format: {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]");

        List<LogDescriptor> descriptors = LogManager.fromHocon(config);
        LogManager manager = new LogManager(descriptors);

        assertThat(manager.getFormat(Paths.get("/sss.log")), nullValue());
        assertThat(manager.getFormat(Paths.get("/aaa/wwww/aaa.log")), notNullValue());
    }

    @Test
    public void testAccess() {
        String file = getClass().getResource("/integration/data/1-100.log").getFile();
        assert file.endsWith("/integration/data/1-100.log");

        String root = file.substring(0, file.length() - "/integration/data/1-100.log".length());

        LogManager manager = new LogManager(LogManager.fromHocon(ConfigFactory.parseString("" +
                "logs = [\n" +
                "{" +
                "  path: \"" + root + "/integration/**/*.log\"\n" +
                "  format= {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]")));

        assertThat(manager.checkAccess(Paths.get("/")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root)), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/1-100.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/zzzz.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/fff/zzzz.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/fff/fff2/zzzz.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/zzzz.log")), nullValue());

        assertThat(manager.checkAccess(Paths.get(root + "/zzzz.log")), notNullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/1-100.logg")), notNullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/1-100.logg")), notNullValue());

        manager = new LogManager(LogManager.fromHocon(ConfigFactory.parseString("" +
                "logs = [" +
                "{\n" +
                "  path: \"" + root + "/integration/*/*.log\"\n" +
                "  format= {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]")));

        assertThat(manager.checkAccess(Paths.get("/")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root)), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/1-100.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/zzzz.log")), nullValue());

        assertThat(manager.checkAccess(Paths.get(root + "/integration/zzzz.log")), notNullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/fff/zzzz.log")), notNullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/fff/fff2/zzzz.log")), notNullValue());

        manager = new LogManager(LogManager.fromHocon(ConfigFactory.parseString("" +
                "logs = [\n" +
                "{" +
                "  path: \"" + root + "/integration/*/*/*.log\"\n" +
                "  format= {type: \"LogbackLogFormat\", pattern: \"%date [%thread] %level %logger - %msg%n\"}" +
                "}" +
                "]")));

        assertThat(manager.checkAccess(Paths.get(root + "/integration/data")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/config")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/fff/zzzz.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/folder")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/folder/zzzz.log")), nullValue());

        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/ffffff2//zzzz.log")), nullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/data/1-100.log")), notNullValue());
        assertThat(manager.checkAccess(Paths.get(root + "/integration/zzzz.log")), notNullValue());
    }
}