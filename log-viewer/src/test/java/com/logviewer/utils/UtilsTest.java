package com.logviewer.utils;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testFileComparision() {
        assert Utils.compareFileNames("abc", "") > 0;
        assert Utils.compareFileNames("abc", "abc") == 0;
        assert Utils.compareFileNames("abc", "abC") == 0;
        assert Utils.compareFileNames("abc", "a") > 0;
        assert Utils.compareFileNames("a10c", "a9c") > 0;
        assert Utils.compareFileNames("a1c", "a9c") < 0;
        assert Utils.compareFileNames("a100000000000000000000000000000000000c", "a9c") > 0;
        assert Utils.compareFileNames("a10b20cc", "a10b20d") < 0;
        assert Utils.compareFileNames("a10b20cc", "a10b20") > 0;
        assert Utils.compareFileNames("a10b20c", "a10b20C") == 0;
        assert Utils.compareFileNames("a10b20c10", "a10b20c3") > 0;
    }

}
