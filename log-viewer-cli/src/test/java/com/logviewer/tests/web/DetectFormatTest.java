package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DetectFormatTest extends AbstractWebTestCase {

    @Test
    public void detectFormat() throws InterruptedException {
        openLog("single-line.log");

        driver.findElement(By.xpath("//span[text()='INFO']"));

        WebElement date = driver.findElement(By.xpath("//span[text()='2017-12-04_13:23:48.324']"));
        String title = date.getAttribute("title");
        assert title.startsWith("2017-12-04");
    }
}
