package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;

public class NavigationIntegrationTest extends AbstractWebTestCase {

    @Test
    public void singleVisibleDirectoryIsExpand() {
        openUrl("/");

        Path emptyLogPath = dataDir.resolve("empty.log");

        WebElement in = driver.findElement(By.cssSelector("input[name=pathToOpen]:focus"));// pathToOpen field must be autofocusiable
        in.sendKeys(emptyLogPath.toString());

        in.submit();

        driver.findElement(By.className("empty-log-message"));
    }


}
