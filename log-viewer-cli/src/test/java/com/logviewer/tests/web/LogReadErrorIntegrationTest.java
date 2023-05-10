package com.logviewer.tests.web;

import com.google.common.collect.Iterables;
import com.logviewer.TestUtils;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.utils.Utils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.logviewer.data2.UnparsableFileErrorTest.LOG_VIEWER_PARSER_MAX_UNPARSABLE_BLOCK_SIZE;
import static org.junit.Assert.assertEquals;

public class LogReadErrorIntegrationTest extends AbstractWebTestCase {

    @Test
    public void noLogPath() {
        openUrl("log");

        WebElement noLogMessage = driver.findElement(MESSAGE_NO_LOG_PATH);

        noLogMessage.findElement(By.linkText("file browser"));
    }

    @Test
    public void noLogPath2() {
        openUrl("log", "path", "");

        WebElement noLogMessage = driver.findElement(MESSAGE_NO_LOG_PATH);

        noLogMessage.findElement(By.linkText("file browser"));
    }

    @Test
    public void logNotFound() {
        openUrl("log", "path", "/notExistingLog");

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Log file does not exist");
    }

    @Test
    public void logNotFoundWrongPath() {
        openUrl("log", "path", "/sdfdfs/sdfsdfsdf/sdfsdfd/notExistingLog");

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Log file does not exist");
    }

    @Test
    public void logNotFound2() {
        setSimpleConfig();

        openUrl("log", "path", "/notExistingLog.log", "path", "/notExists2.log");

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Log file does not exist");
    }

    @Test
    public void emptyLog() {
        openLog("empty.log");

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Log is empty");
    }

    @Test
    public void emptyLog2() throws IOException {
        String emptyLog1 = getDataFilePath("empty.log");
        Path emptyLog2 = tmpDir.resolve("empty2.log");
        Files.write(emptyLog2, Utils.EMPTY_BYTE_ARRAY);

        setSimpleConfig();

        openUrl("log", "path", emptyLog1, "path", emptyLog2.toString());

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Log is empty");
    }

    @Test
    public void emptyLogAndNotFound() {
        String emptyLog1 = getDataFilePath("empty.log");

        setSimpleConfig();

        openUrl("log", "path", emptyLog1, "path", "/mot-found.log");

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Log is empty") : errorMsg.getText();
    }

    @Test
    public void noDateField() {
        String path1 = getDataFilePath("1-7.log");
        String path2 = getDataFilePath("1-100.log");
        openUrl("log", "path", path1, "path", path2);

        WebElement errorMsg = driver.findElement(By.className("no-record-msg"));
        assert errorMsg.getText().contains("Failed to read log");

        assert driver.findElements(By.cssSelector(".file-list .file-error")).size() == 2;
        assertEquals("0", driver.findElement(By.id("successFileCount")).getText());
        assertEquals("2", driver.findElement(By.id("totalFileCount")).getText());
    }

    @Test
    public void breakLog() throws IOException {
        Path tmpLog = tmpDir.resolve("log.log");
        Files.write(tmpLog, IntStream.rangeClosed(11, 100).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        openLog(tmpLog);

        recordByText("100");

        setHeight(10);

        recordByText("62");

        Files.delete(tmpLog);

        new Actions(driver).sendKeys(Keys.PAGE_UP).sendKeys(Keys.PAGE_UP).sendKeys(Keys.PAGE_UP).sendKeys(Keys.PAGE_UP).perform();

        assert !driver.findElement(By.id("records")).isDisplayed();

        driver.findElement(By.cssSelector(".file-not-found"));
    }

    @Test
    public void incorrectFormat() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}] %m"));

        System.setProperty(LOG_VIEWER_PARSER_MAX_UNPARSABLE_BLOCK_SIZE, "150");

        try {
            openLog("search.log");

            WebElement error = Iterables.getOnlyElement(driver.findElements(By.cssSelector(".file-list .file-error")));
            assertEquals("Incorrect log format", error.getText());
        } finally {
            System.clearProperty(LOG_VIEWER_PARSER_MAX_UNPARSABLE_BLOCK_SIZE);
        }
    }

    @Test
    public void noDateFieldInUnexistingLog() {
        String path1 = getDataFilePath("1-7.log");
        String path2 = getDataFilePath("1-100.log");
        openUrl("log", "path", path1 + ".blabla.log", "path", path2 + ".blabla.log");

        driver.findElement(By.className("file-not-found")); // Wait for rendering
        assertEquals(2, driver.findElements(By.className("file-not-found")).size());
    }

    @Test
    public void noDateFieldInRestrictedFiles() {
        ctx.getBean(LvFileAccessManagerImpl.class).setPaths(Collections.emptyList());

        String path1 = getDataFilePath("1-7.log");
        String path2 = getDataFilePath("1-100.log");

        openUrl("log", "path", path1, "path", path2);

        driver.findElement(By.className("file-error")); // Wait for rendering
        List<WebElement> elements = driver.findElements(By.className("file-error"));
        assertEquals(2, elements.size());

        elements.forEach(e -> e.getText().contains("Access Denied"));
    }

    private void setSimpleConfig() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);
    }
}
