package com.logviewer.formats.utils;

import org.junit.Test;
import org.springframework.lang.Nullable;

import static org.junit.Assert.assertEquals;

public abstract class LayoutNodeTestBase {

    protected abstract LvLayoutNode create();

    protected void check(String s, @Nullable String expected) {
        int idx = s.indexOf("<pos>");
        if (idx < 0) {
            idx = 0;
        } else {
            idx += "<pos>".length();
        }

        LvLayoutNode node = create().clone();

        int res = node.parse(s, idx, s.length());

        if (expected == null) {
            assertEquals(LvLayoutNode.PARSE_FAILED, res);
        } else {
            assert res != LvLayoutNode.PARSE_FAILED : "parsing failed: " + s;

            assertEquals(expected, s.substring(idx, res));
        }
    }

    @Test
    public void testClone() {
        LvLayoutNode node = create();

        LvLayoutNode clone = node.clone();

        assert clone.getClass() == node.getClass();

        if (node instanceof LvLayoutCustomTypeNode) {
            assertEquals(((LvLayoutCustomTypeNode)node).getFieldName(), ((LvLayoutCustomTypeNode)clone).getFieldName());
            assertEquals(((LvLayoutCustomTypeNode)node).getFieldType(), ((LvLayoutCustomTypeNode)clone).getFieldType());
        }
    }

}
