package com.logviewer.tests.web;

import com.google.common.collect.Iterables;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class EventDetailsTest extends AbstractWebTestCase {

    @Test
    public void openContextMenuByPointer() {
        openLog("rendering/one-line-exception.log");

        By menuItem = By.xpath("//ul[@class='dropdown-menu show']/li");

        notExist(menuItem);

        WebElement record = driver.findElement(By.className("record"));
        new Actions(driver).moveToElement(record).perform();

        WebElement pointer = record.findElement(By.className("rec-pointer"));

        Point pointerLocation = pointer.getLocation();

        pointer.click();

        WebElement menu = driver.findElement(menuItem);

        assert Math.abs(pointerLocation.y - menu.getLocation().y) < 20;
        assert Math.abs(pointerLocation.x - menu.getLocation().x) < 20;
    }

    @Test
    public void testEventDetails() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new LogbackLogFormat("%d{yyyy-MM-dd_HH:mm:ss.SSS} [%t] %level %logger - %msg%n"));

        openLog("rendering/one-line-exception.log");

        List<WebElement> records = driver.findElementsByClassName("record");
        WebElement rec = Iterables.getOnlyElement(records);

        new Actions(driver).contextClick(rec).perform();

        driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Event details')]")).click();

        List<WebElement> fieldLabels = driver.findElementsByCssSelector("lv-event-details .field .field-label");

        assertEquals(Arrays.asList("date", "thread", "level", "logger", "msg"),
                fieldLabels.stream().map(WebElement::getText).collect(Collectors.toList()));
    }

}
