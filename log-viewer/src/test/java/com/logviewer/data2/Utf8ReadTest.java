package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
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
    private static final LogFormat LOG4J_UTF8 = new Log4jLogFormat(StandardCharsets.UTF_8, "%d{yyyy-MM-dd HH:mm:ss} %msg%n");

    private static final LogFormat LOG4J_ICO8859 = new Log4jLogFormat(StandardCharsets.ISO_8859_1, "%d{yyyy-MM-dd HH:mm:ss} %msg%n");

    private static final LogFormat FORMAT_REGEX = new RegexLogFormat(StandardCharsets.UTF_8,
            "(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d) (.*)",
            false,
            "yyyy-MM-dd HH:mm:ss", "date",
            new RegexLogFormat.RegexField("date", 1, "date"),
            new RegexLogFormat.RegexField("msg", 2, "message")
    );


    @Test
    public void readNonLatinText() throws IOException {
        List<LogRecord> records = loadLog("utf8.log", LOG4J_DEFAULT);

        check(records);

        check(loadLog("utf8.log", LOG4J_UTF8));

        check(loadLog("utf8.log", FORMAT_REGEX));

        List<LogRecord> recordsIco = loadLog("utf8.log", LOG4J_ICO8859);

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
