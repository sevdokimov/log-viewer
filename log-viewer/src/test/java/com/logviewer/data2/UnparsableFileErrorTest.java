package com.logviewer.data2;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import org.junit.Test;

public class UnparsableFileErrorTest extends AbstractLogTest {

    public static final String LOG_VIEWER_PARSER_MAX_UNPARSABLE_BLOCK_SIZE = "log-viewer.parser.max-unparsable-block-size";

    @Test
    public void unparsableError() {
        TestUtils.withSystemProp(LOG_VIEWER_PARSER_MAX_UNPARSABLE_BLOCK_SIZE, "150", () -> {
            try {
                try (Snapshot snapshot = log("/testdata/test-log.log", new Log4jLogFormat("%d %m"))) {
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecords(0, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecords(200, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecords(200, true, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecords(snapshot.getSize() - 4, false, r -> true));

                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(0, false, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(200, true, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(200, false, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(snapshot.getSize(), true, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(snapshot.getSize(), false, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(snapshot.getSize() - 4, true, r -> true));
                    TestUtils.assertError(IncorrectFormatException.class, () -> snapshot.processRecordsBack(snapshot.getSize() - 4, false, r -> true));
                }
            } finally {
                System.clearProperty(LOG_VIEWER_PARSER_MAX_UNPARSABLE_BLOCK_SIZE);
            }
        });
    }

}
