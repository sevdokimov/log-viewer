package com.logviewer.formats.utils;

import com.logviewer.formats.LvLayoutLog4jISO8601DateTest;
import org.junit.Test;

import java.text.ParseException;

public class LvLayoutSimpleDateNodeTest {

    @Test
    public void withTimezone() throws ParseException {
        LvLayoutLog4jISO8601DateTest.checkLayoutWithTimeZone(new LvLayoutLog4jISO8601Date(true));
    }
}