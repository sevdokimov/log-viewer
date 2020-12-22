package com.logviewer.filters;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroovyPredicateTest extends AbstractLogTest {

    private LvPredicateChecker ctx;
    private Record record;

    @Before
    public void before() throws IOException, LogCrashedException {
        Log log = getLogService().openLog(getTestLog("multilog/search.log"), TestUtils.MULTIFILE_LOG_FORMAT);
        ctx = new LvPredicateChecker(log);

        List<Record> list = new ArrayList<>();
        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, list::add);
        }

        record = list.get(0);
    }

    @Test
    public void testSandbox() {
        check("msg == 'zzz a'", true);
        check("msg.endsWith('a')", true);
        check("msg.contains('zzz')", true);
        check("msg.endsWith('!!!')", false);
        check("def m = [a:1]; return m['a'] > 0", true);
        check("def a = [1]; def b = [1]; return a.is(b)", false);

        check("msg ==~ /.+a/", true);
        check("msg ==~ /.+z/", false);

        check("msg in ['111', '222'] || msg in ['__', '__']", false);
        check("def test() {return msg.endsWith('a')}; return test()", true);

        check("" +
                "import groovy.transform.Field;\n" +
                "import java.util.regex.Pattern;\n" +
                "def m = Pattern.compile(/\\d+/).matcher(_);\n" +
                "return m.find()",
                true);

        checkError("System.exit(1)");
        checkError("this.evaluate('ls')");
        checkError("super.evaluate('ls')");

        checkError("System.exit(1)");
        checkError("'ls'.execute()");
        checkError("'ls'.invokeMethod('execute', null)");
        checkError("'ls'.setMetaClass(null)");
    }

    private void check(@NonNull String script, boolean matches) {
        GroovyPredicate groovyPredicate = new GroovyPredicate(script);

        boolean res = groovyPredicate.test(record, ctx);

        Assert.assertEquals(script, matches, res);
    }

    private void checkError(@NonNull String script) {
        GroovyPredicate groovyPredicate = new GroovyPredicate(script);

        TestUtils.assertError(SecurityException.class, () -> groovyPredicate.test(record, ctx));
    }
}
