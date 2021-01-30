package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
}
