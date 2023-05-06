package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.utils.LvGsonUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Utf8ReadTest extends AbstractLogTest {

    private static final LogFormat LOG4J_DEFAULT = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss} %msg%n");
    private static final LogFormat LOG4J_UTF8 = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss} %msg%n").setCharset(StandardCharsets.UTF_8);

    private static final LogFormat LOG4J_ICO8859 = new Log4jLogFormat("%d{yyyy-MM-dd HH:mm:ss} %msg%n").setCharset(StandardCharsets.ISO_8859_1);

    private static final RegexLogFormat FORMAT_REGEX_UTF8 = new RegexLogFormat(
            "(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d) (.*)",
            "yyyy-MM-dd HH:mm:ss", "date",
            new RegexLogFormat.RegexField("date", 1, "date"),
            new RegexLogFormat.RegexField("msg", 2, "message")
    ).setCharset(StandardCharsets.UTF_8);

    private static final LogFormat FORMAT_REGEX_ICO8859 = LvGsonUtils.copy(FORMAT_REGEX_UTF8).setCharset(StandardCharsets.ISO_8859_1);

    @Test
    public void readNonLatinText() throws IOException {
        List<LogRecord> records = loadLog("utf8.log", LOG4J_DEFAULT);

        check(records);

        check(loadLog("utf8.log", LOG4J_UTF8));

        check(loadLog("utf8.log", FORMAT_REGEX_UTF8));

        List<LogRecord> recordsIco = loadLog("utf8.log", LOG4J_ICO8859);

        for (int i = 0, recordsIcoSize = recordsIco.size(); i < recordsIcoSize; i++) {
            LogRecord record = recordsIco.get(i);
            assert record.getFieldText("msg").length() > records.get(i).getFieldText("msg").length();
        }

        recordsIco = loadLog("utf8.log", FORMAT_REGEX_ICO8859);

        for (int i = 0, recordsIcoSize = recordsIco.size(); i < recordsIcoSize; i++) {
            LogRecord record = recordsIco.get(i);
            assert record.getFieldText("msg").length() > records.get(i).getFieldText("msg").length();
        }
    }

    private void check(List<LogRecord> records) {
        assertThat(records.stream().map(r -> r.getFieldText("msg")).collect(Collectors.toList()), is(
                Arrays.asList("текст 1", "текст 2\nыыы", "текст\nтекст 3")
        ));
    }
}
