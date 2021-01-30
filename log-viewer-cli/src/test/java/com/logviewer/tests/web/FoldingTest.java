package com.logviewer.tests.web;

import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class FoldingTest extends AbstractWebTestCase {

    @Test
    public void expanding() {
        openLog("rendering/strange-exception-line.log");

        lastRecord();

        WebElement hiddenLine = driver.findElement(By.xpath("//div[@id='records']/div[@class='record']//div[@class='ex-stacktrace-line'][normalize-space(.)='at ------ submitted from ------.(Unknown Source)']"));

        assert !hiddenLine.isDisplayed();
        driver.findElement(By.cssSelector(".record .coll-expander")).click();
        assert hiddenLine.isDisplayed();
        driver.findElement(By.cssSelector(".record .coll-collapser")).click();
        assert !hiddenLine.isDisplayed();

        WebElement visibleLine = driver.findElement(By.xpath("//div[@id='records']/div[@class='record']//div[@class='ex-stacktrace-line'][normalize-space(.)='at org.apache.catalina.connector.OutputBuffer.doFlush(OutputBuffer.java:370)']"));
        assert visibleLine.isDisplayed();

        select(visibleLine);

        String selectedText = getSelectedText();

        driver.findElement(By.cssSelector(".record .coll-expander")).click();
        assert hiddenLine.isDisplayed();
        driver.findElement(By.cssSelector(".record .coll-collapser")).click();
        assert !hiddenLine.isDisplayed();

        assertEquals(selectedText, getSelectedText());
    }

    @Test
    public void coping() throws IOException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new LogbackLogFormat("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %level %logger %msg%n"));

        String logPath = openLog("rendering/strange-exception-line.log");

        String text = new String(Files.readAllBytes(Paths.get(logPath)));

        select(lastRecord().findElement(By.cssSelector(".rec-text")));

        String textFilterValue = copySelection();

        assertEquals(text, textFilterValue);
    }
}