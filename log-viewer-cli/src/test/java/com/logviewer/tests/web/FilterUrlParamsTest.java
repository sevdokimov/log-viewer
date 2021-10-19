package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

public class FilterUrlParamsTest extends AbstractWebTestCase {

    @Test
    public void filtersFromUrlJson() {
        ThreadFilterTest.setFormat();

        String path = getDataFilePath("thread-filter-test.log");
        openUrl("log",
                "path", path,
                "filters", "{\"date\":{\"endDate\":\"01325361900000000000\",\"startDate\":\"01325361660000000000\"},\"thread\":{\"includes\":[\"exec-*\"]},\"textFilters\":[{\"id\":\"3050352091\",\"pattern\":{\"s\":\"[exec-100] c\"},\"exclude\":true}],\"jsFilters\":[]}");

        waitForRecordsLoading();

        assertEquals("[2012.01.01 00:01][exec-1] b\n[2012.01.01 00:03][exec-100] d", getVisibleRecords());

        notExist(By.tagName("lv-exception-only"));

        WebElement threadFilter = driver.findElement(By.cssSelector("lv-thread-filter"));
        assertEquals("exec-*", threadFilter.getText().trim());

        WebElement dateFilter = driver.findElement(By.cssSelector("lv-date-interval"));
        assertEquals("2012-01-01 00:01 - 2012-01-01 00:05", dateFilter.getText().trim());

        WebElement textFilter = driver.findElement(By.cssSelector("lv-text-filter"));
        assertEquals("Text: [exec-100] c", textFilter.getText().trim());

        assert driver.getCurrentUrl().contains("filters=%7B%22");

        driver.findElement(ADD_FILTER_BUTTON).click();
        driver.findElementById("add-stacktrace-filter").click();
        driver.findElement(By.tagName("lv-exception-only"));

        assert driver.getCurrentUrl().matches(".*filters=[a-f0-9]{32}.*");
    }


}
