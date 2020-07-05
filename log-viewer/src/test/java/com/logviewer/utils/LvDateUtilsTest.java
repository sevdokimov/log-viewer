package com.logviewer.utils;

import org.junit.Test;

import java.text.SimpleDateFormat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LvDateUtilsTest {

    @Test
    public void test() {
        checkFormat("yyyy-MM-dd HH:mm:ss.SSS", true);
        checkFormat("yyyy-MM-dd HH:mm:ss", true);
        checkFormat("yyyy-MM-dd HH:mm", true);
        checkFormat("yyyy-MM-dd", true);
        
        checkFormat("yyyy-MM-dd HH:mm:ss.SSS Z", true);
        checkFormat("yyyy-MM-dd HH:mm:ss Z", true);
        checkFormat("yyyy-MM-dd HH:mm Z", true);
        checkFormat("yyyy-MM-dd Z", true);

        checkFormat("yyyy-MM-dd HH:mm:ss.SSS z", true);
        checkFormat("yyyy-MM-dd HH:mm:ss z", true);
        checkFormat("yyyy-MM-dd HH:mm z", true);
        checkFormat("yyyy-MM-dd z", true);

        checkFormat("yyyy-MM-dd hh:mm a", true);
        checkFormat("yyyy-MM-dd hh:mm", false);

        checkFormat("HH:mm:ss", false);
        checkFormat("yyyy-dd HH:mm:ss", false);
        checkFormat("dd HH:mm:ss", false);
    }

    private void checkFormat(String format, boolean expectedRes) {
        if (expectedRes) {
            assertTrue(format, LvDateUtils.isDateFormatFull(new SimpleDateFormat(format)));
        } else {
            assertFalse(format, LvDateUtils.isDateFormatFull(new SimpleDateFormat(format)));
        }
    }
}
