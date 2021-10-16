package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LogNavigationTest extends AbstractWebTestCase {

    @Test
    public void testRemovingInvisibleRecords() {
        openLog("1-100.log");
        setHeight(5);
        driver.navigate().refresh();

        checkRemoveInvisibleRecordsFromBottom();
        checkRemoveInvisibleRecordsFromTop();
    }

    private void checkRemoveInvisibleRecordsFromBottom() {
        checkLastRecord("100");

        List<WebElement> records = driver.findElements(By.xpath("//div[@id='records']/div[@class='record']"));
        assert records.size() >= 10 && records.size() <= 12;

        for (int i = 0; i < 20; i++) {
            new Actions(driver).sendKeys(Keys.UP).perform();
        }

        recordByText("78");

        records = driver.findElements(By.xpath("//div[@id='records']/div[@class='record']"));
        assert records.size() >= 20 && records.size() <= 25;
    }

    private void checkRemoveInvisibleRecordsFromTop() {
        new Actions(driver).sendKeys(Keys.HOME).perform();

        List<WebElement> records = driver.findElements(By.xpath("//div[@id='records']/div[@class='record']"));
        assert records.size() >= 10 && records.size() <= 12;

        assertEquals("1", records.get(0).getText());

        for (int i = 0; i < 20; i++) {
            new Actions(driver).sendKeys(Keys.DOWN).perform();
        }

        recordByText("24");

        records = driver.findElements(By.xpath("//div[@id='records']/div[@class='record']"));
        assert records.size() >= 20 && records.size() <= 25;
    }

}
