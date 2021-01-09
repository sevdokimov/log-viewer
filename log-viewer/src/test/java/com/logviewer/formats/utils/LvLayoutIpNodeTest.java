package com.logviewer.formats.utils;

import org.junit.Test;

public class LvLayoutIpNodeTest extends LayoutNodeTestBase {

    @Test
    public void testParse() {
        check(" 30.1.22.3", null);
        check(" <pos>30.1.22.3", "30.1.22.3");

        check("0.0.0.0", "0.0.0.0");
        check("255.255.255.255", "255.255.255.255");
        check("99.99.99.99", "99.99.99.99");
        check("000.000.000.000", "000.000.000.000");

        check("__<pos>99.99.99.99aaa", "99.99.99.99");
        check("__<pos>99.99.99.99[", "99.99.99.99");
        check("__<pos>99.99.99.99.", "99.99.99.99");
        check("__<pos>99.99.99.111", "99.99.99.111");
        check("__<pos>99.99.99.1110", null);
        check("__<pos>99.99.99.1119", null);
        check("__<pos>99.99.99.111_", "99.99.99.111");

        check("30.1.22.111", "30.1.22.111");
        check("30.1.22.11", "30.1.22.11");
        check("30.1.22.1", "30.1.22.1");
        check("30.1.22.", null);
        check("30.1.22", null);
        check("30.1.2", null);
        check("30.1.", null);
        check("30.1", null);
        check("30.", null);
        check("30", null);
        check("3", null);
        check("", null);
    }

    protected LvLayoutNode create() {
        return new LvLayoutIpNode("fff", "ttt");
    }
}