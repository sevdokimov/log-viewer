package com.logviewer.filters;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.Log;
import com.logviewer.data2.LvPredicateChecker;
import com.logviewer.data2.Record;
import com.logviewer.data2.Snapshot;
import org.intellij.lang.annotations.Language;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.EcmaError;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsPredicateTest extends AbstractLogTest {

    private LvPredicateChecker ctx;
    private Record record;

    @Before
    public void before() throws IOException {
        Log log = getLogService().openLog(getTestLog("multilog/search.log"), TestUtils.MULTIFILE_LOG_FORMAT);
        ctx = new LvPredicateChecker(log);

        List<Record> list = new ArrayList<>();
        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, list::add);
        }

        record = list.get(0);
    }

    @After
    public void destroy() throws Exception {
        ctx.close();
    }

    private void check(@NonNull @Language("JavaScript") String script, boolean matches) {
        JsPredicate predicate = new JsPredicate(script);

        boolean res = predicate.test(record, ctx);

        Assert.assertEquals(script, matches, res);
    }

    private void checkError(@Language("JavaScript") @NonNull String script, @Nullable String msg) {
        checkError(script, EcmaError.class, msg);
    }

    private void checkError(@NonNull String script, @NonNull Class<? extends Throwable> errorType, @Nullable String msg) {
        JsPredicate predicate = new JsPredicate(script);

        Throwable error = TestUtils.assertError(errorType, () -> predicate.test(record, ctx));

        if (msg != null)
            assert error.getMessage().contains(msg);
    }

    @Test
    public void test() {
        check("function isVisible(text, fields) {return null}", false);
        check("function isVisible(text, fields) {return undefined}", false);
        check("function isVisible(text, fields) {return false}", false);
        check("function isVisible(text, fields) {return true}", true);
        check("function isVisible(text, fields) {return 0}", false);
        check("function isVisible(text, fields) {return 0.1}", true);

        check("function isVisible(text, fields) {return '0'}", true);
        check("function isVisible(text, fields) {return 'abc'}", true);
        check("function isVisible(text, fields) {return ''}", false);
        check("function isVisible(text, fields) {return {a: 11}}", true);
        check("function isVisible(text, fields) {return {}}", true);
        check("function isVisible(text, fields) {return [1, 2, 3]}", true);
        check("function isVisible(text, fields) {return []}", true);

        check("function isVisible(text, fields) {return ['11', '22'].find(i => i.length >= 2)}", true);
        check("function isVisible(text, fields) {return ['11', '22'].find(i => i.length >= 20)}", false);

        check("function isVisible(text, fields) {}", false);

        check("function isVisible(text, fields) { return text.includes('zzz') }", true);
        check("function isVisible(text, fields) { return fields.msg.includes('zzz') }", true);
        check("function isVisible(text, fields) { return text.includes('dfsfskjflds453453') }", false);
        check("function isVisible(text, fields) { return fields.msg.includes('dfsfskjflds453453') }", false);
        check("function isVisible(text, fields) { val = 'zzz'; return fields.msg.includes(val) }", true);
        check("function isVisible(text, fields) { var val = 'zzz'; return fields.msg.includes(val) }", true);
        check("function isVisible(text, fields) { val = 'fghk'; return fields.msg.includes(val) }", false);
        check("function isVisible(text, fields) { var val = 'fghk'; return fields.msg.includes(val) }", false);
        check("function isVisible(text, fields) { var r = /z+/; return fields.msg.match(r) }", true);
        check("function isVisible(text, fields) { var r = /^z+text/; return fields.msg.match(r) }", false);
        check("function isVisible(text, fields) { return /^z+text/.test(text) }", false);
        check("function isVisible(text, fields) { return /Z+/i.test(text) }", true);

        check("function isVisible(text, fields) {\n" +
                "var match = text.match(/^\\d{6} (\\d\\d):/)\n" +
                "if (!match) return false;" +
                "return parseInt(match[1]) < 11" +
                "\n}", true);

        check("function isVisible(text, fields) {\n" +
                "var match = text.match(/^\\d{6} (\\d\\d):/)\n" +
                "if (!match) return false;" +
                "return parseInt(match[1]) < 9" +
                "\n}", false);

        check("function isVisible(text, fields) {\n" +
                "    function isGood(s, a) {\n" +
                "        return s.length > 0 && s[0] === a" +
                "    }\n" +
                "\n" +
                "    return isGood(fields.msg, 'z') && !isGood(fields.msg, '0');\n" +
                "}", true);

        checkError("function isVisible(text, fields) { return text.contains('dfsfskjflds453453') }", "contains");
        checkError("function isVisible(text, fields) { return rrr355.includes('dfsfskjflds453453') }", "rrr355");
        checkError("text.includes('dfsfskjflds453453')", IllegalArgumentException.class, "Script must be a function");
    }
}