package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FilterPanelTest extends AbstractWebTestCase {

    @Test
    public void unknownLog() {
        openLog("1-7.log");
        waitForRecordsLoading();

        driver.findElement(By.tagName("lv-top-filter"));

        notExist(By.tagName("lv-exception-only"));
        notExist(By.tagName("lv-date-interval"));
        notExist(By.tagName("lv-thread-filter"));

        addFilterMenuClick();

        assertThat(driver.findElements(By.cssSelector(".add-filter-menu .dropdown-menu a")).size(), is(3));
    }

    @Test
    public void addFilters() {
        ThreadFilterTest.setFormat();
        openLog("thread-filter-test.log");
        waitForRecordsLoading();

        driver.findElement(By.tagName("lv-top-filter"));

        notExist(By.tagName("lv-exception-only"));
        notExist(By.tagName("lv-date-interval"));
        notExist(By.tagName("lv-thread-filter"));

        addFilterMenuClick();
        assertThat(driver.findElements(By.cssSelector(".add-filter-menu .dropdown-menu a")).size(), is(5));

        driver.findElement(By.id("add-date-filter")).click();

        assert !driver.findElement(By.cssSelector(".add-filter-menu .dropdown-menu")).isDisplayed(); // menu disappeared.

        driver.findElement(By.cssSelector("lv-date-interval .lv-dropdown-panel"));

        addFilterMenuClick();
        assertThat(driver.findElements(By.cssSelector(".add-filter-menu .dropdown-menu a")).size(), is(4));
        driver.findElement(By.id("add-thread-filter")).click();
        driver.findElement(By.cssSelector("lv-thread-filter .lv-dropdown-panel"));

        addFilterMenuClick();
        assertThat(driver.findElements(By.cssSelector(".add-filter-menu .dropdown-menu a")).size(), is(3));
        driver.findElement(By.id("add-stacktrace-filter")).click();
        driver.findElement(By.tagName("lv-exception-only"));

        addFilterMenuClick();
        assertThat(driver.findElements(By.cssSelector(".add-filter-menu .dropdown-menu a")).size(), is(2));
    }

    private void addFilterMenuClick() {
        driver.findElement(ADD_FILTER_BUTTON).click();
    }


}
