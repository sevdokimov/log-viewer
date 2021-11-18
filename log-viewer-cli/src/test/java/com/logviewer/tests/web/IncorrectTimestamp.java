package com.logviewer.tests.web;

import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class IncorrectTimestamp extends AbstractWebTestCase {

    @Test
    public void testIncorrectTimestamp() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}][%t] %m"));

        openLog("incorrect-timestamp.log");
        setHeight(5);
        driver.navigate().refresh();

        checkLastRecord("[2012.01.01 00:10][::::::::::] sss 49 49");

        new Actions(driver).sendKeys(Keys.HOME).perform();
        WebElement firstRecord = recordByText("[2012.01.01 00:59][         a] sss 0 0");

        List<WebElement> records = getRecord();
        assert firstRecord.equals(records.get(0));

        checkReversedOrder(records);

        while (!records.get(records.size() - 1).getText().equals("[2012.01.01 00:10][::::::::::] sss 49 49")) {
            new Actions(driver).sendKeys(Keys.PAGE_DOWN).perform();

            records = getRecord();
        }

        // Check filter.
        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys("2012.01.01 00:50");

        driver.findElementById("findPrevArrow").click();
        driver.findElement(By.xpath("//span[@class='search-result'][normalize-space(.)='2012.01.01 00:50']"));
        
        checkReversedOrder(getRecord());
    }

    private void checkReversedOrder(List<WebElement> records) {
        for (int i = 1; i < records.size(); i++) {
            if (records.get(i).isDisplayed() && records.get(i - 1).isDisplayed()) {
                String prev = records.get(i - 1).getText();
                String current = records.get(i).getText();

                assertTrue(prev.compareTo(current) > 0);
            }
        }
    }

}
