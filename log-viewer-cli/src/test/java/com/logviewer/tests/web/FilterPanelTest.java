package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FilterPanelTest extends AbstractWebTestCase {

    @Test
    public void unknownLog() {
        openLog("1-7.log");

        driver.findElementByTagName("lv-top-filter");

        notExist(By.tagName("lv-exception-only"));
        notExist(By.tagName("lv-date-interval"));
        notExist(By.tagName("lv-thread-filter"));

        addFilterMenuClick();

        assertThat(driver.findElementsByCssSelector(".add-filter-menu .dropdown-menu a").size(), is(1));
    }

    @Test
    public void addFilters() {
        ThreadFilterTest.setFormat();
        openLog("thread-filter-test.log");

        driver.findElementByTagName("lv-top-filter");

        notExist(By.tagName("lv-exception-only"));
        notExist(By.tagName("lv-date-interval"));
        notExist(By.tagName("lv-thread-filter"));

        addFilterMenuClick();
        assertThat(driver.findElementsByCssSelector(".add-filter-menu .dropdown-menu a").size(), is(3));

        assert !driver.findElementByClassName("no-filters-to-add").isDisplayed();

        driver.findElementById("add-date-filter").click();

        assert !driver.findElement(By.cssSelector(".add-filter-menu .dropdown-menu")).isDisplayed(); // menu disappeared.

        driver.findElement(By.cssSelector("lv-date-interval .lv-dropdown-panel"));

        addFilterMenuClick();
        assertThat(driver.findElementsByCssSelector(".add-filter-menu .dropdown-menu a").size(), is(2));
        driver.findElementById("add-thread-filter").click();
        driver.findElement(By.cssSelector("lv-thread-filter .lv-dropdown-panel"));

        addFilterMenuClick();
        assertThat(driver.findElementsByCssSelector(".add-filter-menu .dropdown-menu a").size(), is(1));
        driver.findElementById("add-stacktrace-filter").click();
        driver.findElement(By.tagName("lv-exception-only"));

        addFilterMenuClick();
        driver.findElementByClassName("no-filters-to-add").isDisplayed();
    }

    private void addFilterMenuClick() {
        driver.findElementByCssSelector(".add-filter-menu .fa-plus").click();
    }


}
