package com.logviewer.formats.utils;

import org.junit.Test;

import static com.logviewer.formats.utils.LvLayoutNode.PARSE_FAILED;
import static org.junit.Assert.*;

public class LvLayoutTextNodeTest {

    @Test
    public void testMultichar() {
        LvLayoutTextNode node = LvLayoutTextNode.of(" ,,,");

        assertFalse(node.removeSpacesBefore());

        assertEquals(PARSE_FAILED, node.parse(",,, ", 0, 4));
        assertEquals(PARSE_FAILED, node.parse(" ,,z", 0, 4));
        assertEquals(PARSE_FAILED, node.parse("  ,,, ", 0, 4));
    }

    @Test
    public void testSpacesOnly() {
        LvLayoutTextNode node = LvLayoutTextNode.of("   ");

        assertFalse(node.removeSpacesBefore());

        assertEquals(PARSE_FAILED, node.parse("       ", 0, 2));
        assertEquals(PARSE_FAILED, node.parse("       ", 6, 6));
        assertEquals(PARSE_FAILED, node.parse("    ,,z", 2, 6));
        
        assertEquals(3, node.parse("      ", 0, 6));
        assertEquals(4, node.parse("      ", 1, 6));
    }

    @Test
    public void testOneChar() {
        LvLayoutTextNode node = LvLayoutTextNode.of(",");

        assertTrue(node.removeSpacesBefore());

        assertEquals(PARSE_FAILED, node.parse(" ,", 0, 2));
        assertEquals(PARSE_FAILED, node.parse(",r", 1, 2));

        assertEquals(3, node.parse("  ,,", 2, 4));

        assertEquals(2, node.parse("!,", 1, 2));
    }
}