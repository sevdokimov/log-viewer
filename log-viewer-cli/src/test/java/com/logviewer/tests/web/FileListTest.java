package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class FileListTest extends AbstractWebTestCase {

    @Test
    public void fileList() throws InterruptedException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new LogbackLogFormat("[%d{yyyy.MM.dd HH:mm}]%message%n"));

        String path = getDataFilePath("search.log");
        openUrl("log", "path", path, "path", "/unexist_.log");
        setHeight(5);

        WebElement fileStatDropdown = driver.findElementById("file-stat-dropdown");
        fileStatDropdown.click();

        driver.findElementsByCssSelector("sl-log-list-panel .file-not-found");

        new Actions(driver).sendKeys(Keys.HOME).perform();

        Thread.sleep(300);

        driver.findElementsByCssSelector("sl-log-list-panel .file-not-found");
    }
}
