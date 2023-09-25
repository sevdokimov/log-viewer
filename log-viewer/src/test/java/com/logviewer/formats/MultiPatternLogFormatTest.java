package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.LogFormat;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.utils.LvGsonUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class MultiPatternLogFormatTest extends AbstractLogTest {

    @Test
    public void validationEmptyPattern1() {
        String serializedFormat = "{\n" +
                "  \"type\": \"Log4jLogFormat\",\n" +
                "  \"realLog4j\": false,\n" +
                "  \"charset\": \"ISO-8859-1\",\n" +
                "  \"locale\": \"ru_RU\",\n" +
                "  \"customLevels\": [\"XXX\", \"YYY\"]\n" +
//                "  \"pattern\": \"%d{yyyy-MM-dd HH:mm:ss Z} %m%n\"\n" +
                "}\n";

        Log4jLogFormat logFormat = (Log4jLogFormat) LvGsonUtils.GSON.fromJson(serializedFormat, LogFormat.class);

        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.validate());
        assertThat(e.getMessage()).contains("'pattern'", "'patterns'");
    }

    @Test
    public void validationEmptyPattern2() {
        String serializedFormat = "{\n" +
                "  \"type\": \"Log4jLogFormat\",\n" +
                "  \"realLog4j\": false,\n" +
                "  \"charset\": \"ISO-8859-1\",\n" +
                "  \"locale\": \"ru_RU\",\n" +
                "  \"customLevels\": [\"XXX\", \"YYY\"],\n" +
                "  \"pattern\": \"\",\n" +
                "  \"patterns\": []\n" +
                "}\n";

        Log4jLogFormat logFormat = (Log4jLogFormat) LvGsonUtils.GSON.fromJson(serializedFormat, LogFormat.class);

        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.validate());
        assertThat(e.getMessage()).contains("'pattern'", "'patterns'");
    }

    @Test
    public void validationBothFieldFilled() {
        String serializedFormat = "{\n" +
                "  \"type\": \"Log4jLogFormat\",\n" +
                "  \"realLog4j\": false,\n" +
                "  \"charset\": \"ISO-8859-1\",\n" +
                "  \"locale\": \"ru_RU\",\n" +
                "  \"customLevels\": [\"XXX\", \"YYY\"],\n" +
                "  \"pattern\": \"%d{yyyy-MM-dd HH:mm:ss Z} %m%n\",\n" +
                "  \"patterns\": [\"%d{yyyy-MM-dd HH:mm:ss Z} %m%n\"]\n" +
                "}\n";

        Log4jLogFormat logFormat = (Log4jLogFormat) LvGsonUtils.GSON.fromJson(serializedFormat, LogFormat.class);

        IllegalArgumentException e = TestUtils.assertError(IllegalArgumentException.class, () -> logFormat.validate());
        assertThat(e.getMessage()).contains("'pattern'", "'patterns'");
    }

    @Test
    public void mergeFields() {
        LogbackLogFormat format = new LogbackLogFormat("%d %p %m%n", "%d %c (%d) %m %X{clientIp}%n", "%d %c %p (%d) %m %X{xxx}%n");

        assertEquals(Arrays.asList("date", "logger", "level", "msg", "date_1"),
                Stream.of(format.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));

        assertTrue(format.hasFullDate());
        
        LogbackLogFormat format2 = new LogbackLogFormat("%d %p %m%n", "%d %c (%d) %m %X{clientIp}%n", "%d %p %c (%d) %m %X{xxx}%n");

        assertEquals(Arrays.asList("date", "level", "logger", "msg", "date_1"),
                Stream.of(format2.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));
    }

    @Test
    public void noFullDate() {
        LogbackLogFormat format = new LogbackLogFormat("%d %p %m%n", "%d{HH:mm:ss} %c %m%n");

        assertEquals(Arrays.asList("date", "level", "logger", "msg"),
                Stream.of(format.getFields()).map(LogFormat.FieldDescriptor::name).collect(Collectors.toList()));

        assertFalse(format.hasFullDate());
    }
}