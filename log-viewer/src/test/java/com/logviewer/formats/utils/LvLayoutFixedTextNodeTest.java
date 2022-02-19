package com.logviewer.formats.utils;

import org.junit.Test;

import static com.logviewer.formats.utils.LvLayoutNode.PARSE_FAILED;
import static org.junit.Assert.assertEquals;

public class LvLayoutFixedTextNodeTest {

    @Test
    public void testLengthLimit() {
        LvLayoutFixedTextNode node = new LvLayoutFixedTextNode("f", null, "aaa");

        assertEquals(PARSE_FAILED, node.parse("aaaa", 0, 2));
    }
}