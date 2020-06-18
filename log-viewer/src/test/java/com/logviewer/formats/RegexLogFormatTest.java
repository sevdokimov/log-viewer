package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Record;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class RegexLogFormatTest extends AbstractLogTest {

    @Test
    public void testNamedGroupFields() {
        LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8, "msg", "(?<date>\\d+) (?<msg>.+)",
                RegexLogFormat.field("date", null),
                RegexLogFormat.field("msg", null)
                );

        Record record = read(logFormat, "111 message text");

        Assert.assertEquals("111", record.getFieldText(logFormat.getFieldIndexByName("date")));
        Assert.assertEquals("message text", record.getFieldText(logFormat.getFieldIndexByName("msg")));
    }

}
