package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;

public class LogViewUrlTest extends AbstractWebTestCase {
    @Test
    public void openFilesViaDifferentUrlParameters() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);

        String path1 = getDataFilePath("multifile/log-a.log");
        String path2 = getDataFilePath("multifile/log-b.log");

        openUrl("log", "log", path1, "log", path2);
        assertEquals(9, driver.findElements(By.cssSelector("#records > .record")).size());

        openUrl("log", "path", path1, "path", path2);
        assertEquals(9, driver.findElements(By.cssSelector("#records > .record")).size());

        openUrl("log", "path", path1, "log", path2);
        assertEquals(9, driver.findElements(By.cssSelector("#records > .record")).size());

        openUrl("log", "path", path1);
        assertEquals(4, driver.findElements(By.cssSelector("#records > .record")).size());

        openUrl("log", "log", path1);
        assertEquals(4, driver.findElements(By.cssSelector("#records > .record")).size());
    }

}
