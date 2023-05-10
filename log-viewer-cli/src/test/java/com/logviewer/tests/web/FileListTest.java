package com.logviewer.tests.web;

import com.google.common.collect.Iterables;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.utils.TestLogFormats;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileListTest extends AbstractWebTestCase {

    private static void checkFileListHeader(String expectedText) {
        driver.findElement(By.xpath("//*[@id='file-stat-dropdown'][normalize-space(.)='" + expectedText + "']"));
    }

    @Test
    public void oneValidLog() {
        openLog("search.log");
        checkFileListHeader("1 log");
    }

    @Test
    public void twoValidLog() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        openLog("multifile/log-a.log", "multifile/log-b.log");
        checkFileListHeader("2 logs");
    }

    @Test
    public void notFound2Files() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.SEARCH);

        String path = getDataFilePath("search.log");
        openUrl("log", "path", path, "path", "/unexist_.log", "path", "unexisted_non_absolute");

        checkFileListHeader("1 / 3 logs");

        WebElement fileStatDropdown = driver.findElement(By.id("file-stat-dropdown"));

        assertThat(fileStatDropdown.findElement(By.cssSelector("#successFileCount.has-problems-files")).getText(), is("1"));

        fileStatDropdown.click();

        assert driver.findElements(By.cssSelector("lv-log-list-panel lv-file-status .file-not-found")).size() == 2;
    }

    @Test
    public void oneInvalidLog() {
        openLog("multifile");

        checkFileListHeader("0 / 1 log");

        WebElement fileStatDropdown = driver.findElement(By.id("file-stat-dropdown"));
        fileStatDropdown.click();

        WebElement status = Iterables.getOnlyElement(driver.findElements(By.cssSelector("lv-log-list-panel lv-file-status")));
        assertThat(status.getText(), is("IO Error"));

        status.findElement(By.cssSelector(".fa-wrench")).click(); // Show Details icon

        WebElement stacktracePanel = driver.findElement(By.cssSelector(".modal-body .stacktrace-panel"));
        assertThat(stacktracePanel.getText(), containsString("Not a file"));
    }
}
