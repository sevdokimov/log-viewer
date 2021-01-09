package com.logviewer.logLibs.nginx;

import com.logviewer.formats.utils.LayoutNodeTestBase;
import com.logviewer.formats.utils.LvLayoutNode;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.lang.Nullable;

import static org.junit.Assert.assertEquals;

public class NginxStretchNodeTest extends LayoutNodeTestBase {

    @Test
    public void testClone() {
        NginxStretchNode node = new NginxStretchNode("fff", "ttt", false);

        NginxStretchNode clone = node.clone();

        MatcherAssert.assertThat(clone, CoreMatchers.instanceOf(NginxStretchNode.class));

        assertEquals(node.getFieldName(), clone.getFieldName());
        assertEquals(node.getFieldType(), clone.getFieldType());
        assertEquals(node.getMinSize(), clone.getMinSize());
    }

    @Test
    public void testMinSize() {
        NginxStretchNode node = new NginxStretchNode("fff", "ttt", false, 3);

        String s = "__123456";
        if (!node.reset(s, 2, s.length())) {
            throw new IllegalStateException();
        }

        assertEquals("123", s.substring(node.getStart(), node.getEnd()));

        if (node.reset(s, 6, s.length())) {
            throw new IllegalStateException();
        }
    }

    @Test
    public void testStartFromEscape1() {
        NginxStretchNode node = new NginxStretchNode("fff", "ttt", false, 2);

        String s = "\\u11110";
        if (!node.reset(s, 0, s.length())) {
            throw new IllegalStateException();
        }

        assertEquals("\\u1111", s.substring(node.getStart(), node.getEnd()));
    }

    @Test
    public void testStartFromEscape2() {
        NginxStretchNode node = new NginxStretchNode("fff", "ttt", false, 0);

        String s = "\\u11110";
        if (!node.reset(s, 0, s.length())) {
            throw new IllegalStateException();
        }

        assertEquals("", s.substring(node.getStart(), node.getEnd()));
    }

    @Test
    public void testGrow() {
        checkGrow("[a]", "a");
        checkGrow("[\\]", "\\");
        checkGrow("[\\:]", "\\:");
        checkGrow("[\\]:", "\\:");
        checkGrow(" [\\]:", "\\:");
        checkGrow("[\\]\\:", "\\\\");
        checkGrow(" [aaa] ", "aaa");
        checkGrow("[aaa]", "aaa");
        checkGrow("[aaa\\]", "aaa\\");
        checkGrow("[aaa\\u]", "aaa\\u");
        checkGrow("[aaa\\]u", "aaa\\u");
        checkGrow("[aa\\\\]:", "aa\\\\");
        checkGrow("[aa\\\\\\]:", "aa\\\\\\:");

        checkGrow("[aa]\\x550", "aa");
        checkGrow("[aa\\]x550", "aa\\x55");
        checkGrow("[aa\\x]550", "aa\\x55");
        checkGrow("[aa\\x5]50", "aa\\x55");
        checkGrow("[aa\\x55]0", "aa\\x55");

        checkGrow("[aa]\\u55110", "aa");
        checkGrow("[aa\\]u55110", "aa\\u5511");
        checkGrow("[aa\\u]55110", "aa\\u5511");
        checkGrow("[aa\\u5]5110", "aa\\u5511");
        checkGrow("[aa\\u55]110", "aa\\u5511");
    }

    private void checkGrow(String s, @Nullable String expected) {
        int startIdx = s.indexOf('[');
        assert s.lastIndexOf('[') == startIdx;

        int endIdx = s.indexOf(']');
        assert s.lastIndexOf(']') == endIdx;

        s = s.substring(0, startIdx) + s.substring(startIdx + 1, endIdx) + s.substring(endIdx + 1);
        endIdx--;

        NginxStretchNode node = new NginxStretchNode("fff", "ttt", false);

        if (!node.reset(s, startIdx, s.length())) {
            throw new RuntimeException();
        }

        if (expected == null) {
            if (node.grow(s, endIdx, s.length())) {
                throw new IllegalStateException(s);
            }
        } else {
            if (!node.grow(s, endIdx, s.length())) {
                throw new IllegalStateException(s);
            }

            assertEquals(expected, s.substring(node.getStart(), node.getEnd()));
        }
    }

    @Override
    protected LvLayoutNode create() {
        return new NginxStretchNode("fff", "ttt", false);
    }
}