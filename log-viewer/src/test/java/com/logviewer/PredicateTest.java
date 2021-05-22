package com.logviewer;

import com.logviewer.data2.*;
import com.logviewer.filters.CompositeRecordPredicate;
import com.logviewer.filters.FieldArgPredicate;
import com.logviewer.filters.FieldValueSetPredicate;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.formats.RegexLogFormat.RegexField;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class PredicateTest extends AbstractLogTest {

    private final static LogFormat logFormat = new RegexLogFormat(StandardCharsets.UTF_8,
            "\\[([A-Z]+)\\] (.+)? (\\d+)", true,
            new RegexField("level", 1, FieldTypes.LEVEL_LOGBACK), new RegexField("msg", 2), new RegexField("index", 3));

    private LogFilterContext filterContext;

    private List<Record> records;

    @Before
    public void loadLog() throws IOException {
        String logFile = getTestClassLog();

        Log log = getLogService().openLog(logFile, logFormat);

        filterContext = new LvPredicateChecker(log);

        records = new ArrayList<>();
        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, record -> {
                records.add(record);
                return true;
            });
        }
    }

    private void assertRecordEquals(List<Record> records, RecordPredicate predicate, String ... res) {
        assertEquals(Arrays.asList(res), records.stream()
                .filter(r -> predicate.test(r, filterContext))
                .map(r -> filterContext.getFieldValue(r, "index"))
                .collect(Collectors.toList()));
    }

    @Test
    public void filterField() {
        assertRecordEquals(records, new FieldArgPredicate("level", "ERROR"), "4", "5");
        assertRecordEquals(records, new FieldArgPredicate("level", "error", FieldArgPredicate.Operator.IEQUALS), "4", "5");
        assertRecordEquals(records, new FieldArgPredicate("msg", "w", FieldArgPredicate.Operator.CONTAINS), "1");
        assertRecordEquals(records, new FieldArgPredicate("msg", null, FieldArgPredicate.Operator.EQUALS), "5");
        assertRecordEquals(records, new FieldArgPredicate("msg", null, FieldArgPredicate.Operator.NOT_EQUALS), "1", "2", "3", "4");
        assertRecordEquals(records, new FieldArgPredicate("msg", null, FieldArgPredicate.Operator.NOT_IEQUALS), "1", "2", "3", "4");
    }

    @Test
    public void compositeFilters() {
        assertRecordEquals(records,
                new CompositeRecordPredicate(true, new FieldArgPredicate("level", "INFO"), new FieldArgPredicate("msg", "i", FieldArgPredicate.Operator.CONTAINS)),
                "2");

        assertRecordEquals(records,
                new CompositeRecordPredicate(false, new FieldArgPredicate("level", "INFO"), new FieldArgPredicate("msg", "i", FieldArgPredicate.Operator.CONTAINS)),
                "2", "3", "4");
    }

    @Test
    public void fieldSetPredicate() {
        assertRecordEquals(records, new FieldValueSetPredicate(FieldTypes.LEVEL_LOGBACK, Arrays.asList("INFO", "WARN", "ZZZ")),
                "1", "2", "3");
    }
}
