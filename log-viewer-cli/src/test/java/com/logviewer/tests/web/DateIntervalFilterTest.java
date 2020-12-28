package com.logviewer.tests.web;

import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.utils.FilterPanelState;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DateIntervalFilterTest extends AbstractWebTestCase {

    @Test
    public void noFilterIfLogHasNoTimestamp() {
        openLog("1-7.log");

        driver.findElementByTagName("lv-top-filter");

        notExist(By.tagName("lv-date-interval"));

        new Actions(driver).contextClick(lastRecord()).perform();

        driver.findElementByXPath("//ul[@class='dropdown-menu show']/li[contains(., 'Event details')]");
        notExist(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'older')]"));
    }

    private String dateFilterHeader() {
        return driver.findElementByTagName("lv-date-interval").getText().trim();
    }

    @Test
    public void filterTitleEmpty() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setDate(new FilterPanelState.DateFilter()));
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));

        openLog("search.log");

        assertThat(dateFilterHeader(), is("Empty timestamp filter"));
    }

    @Test
    public void filterTitleSince() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setStartDate(date("20120101 00:42:00")));

        openLog("search.log");

        assertThat(dateFilterHeader(), is("Since 2012-01-01 00:42"));
        assert getRecord().size() == 3;
    }

    @Test
    public void filterTitleSinceWithSeconds() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setStartDate(date("20120101 00:42:01")));

        openLog("search.log");

        assertThat(dateFilterHeader(), is("Since 2012-01-01 00:42:01"));
        assert getRecord().size() == 2;
    }

    @Test
    public void filterTitleSinceWithMilliseconds() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setStartDate(date("20120101 00:42:00.111")));

        openLog("search.log");

        assertThat(dateFilterHeader(), is("Since 2012-01-01 00:42:00.111"));
        assert getRecord().size() == 2;
    }

    @Test
    public void filterTitleUntil() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setEndDate(date("20120101 00:02")));

        openLog("search.log");

        assertThat(dateFilterHeader(), is("Till 2012-01-01 00:02"));
        assert getRecord().size() == 3;
    }

    @Test
    public void filterTitleRange() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState()
                .setStartDate(date("20120101 00:02"))
                .setEndDate(date("20120101 00:05")));

        openLog("search.log");

        assertThat(dateFilterHeader(), is("2012-01-01 00:02 - 2012-01-01 00:05"));
        assert getRecord().size() == 4;
    }

    private WebElement timeSelectPanel() {
        return driver.findElementByClassName("time-select-panel");
    }

    @Test
    public void typeDateRestriction() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setDate(new FilterPanelState.DateFilter()));

        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));

        openLog("search.log");

        driver.findElementByTagName("lv-date-interval").click();

        assertThat(timeSelectPanel().findElement(By.name("startDate")).getAttribute("value"), is(""));
        assertThat(timeSelectPanel().findElement(By.name("endDate")).getAttribute("value"), is(""));

        timeSelectPanel().findElement(By.name("startDate")).click();

        new Actions(driver).sendKeys("2012-01-01 00:39").sendKeys(Keys.ENTER).perform();

        notExist(By.className("time-select-panel"));

        assert getRecord().size() == 6;

        driver.findElementByTagName("lv-date-interval").click();

        assertThat(timeSelectPanel().findElement(By.name("startDate")).getAttribute("value"), is("2012-01-01 00:39:00"));
        assertThat(timeSelectPanel().findElement(By.name("endDate")).getAttribute("value"), is(""));

        timeSelectPanel().findElement(By.name("endDate")).click();

        new Actions(driver).sendKeys("2012-01-01 00:42").sendKeys(Keys.ENTER).perform();

        notExist(By.className("time-select-panel"));
        assert getRecord().size() == 4;

        driver.findElementByTagName("lv-date-interval").click();
        assertThat(timeSelectPanel().findElement(By.name("endDate")).getAttribute("value"), is("2012-01-01 00:42:00"));
    }

    @Test
    public void applyCancelButton() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setDate(new FilterPanelState.DateFilter()));
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));

        openLog("search.log");

        driver.findElementByTagName("lv-date-interval").click();
        timeSelectPanel().findElement(By.name("startDate")).sendKeys("2012-01-01 00:39");

        timeSelectPanel().findElement(By.name("apply-button")).click();

        notExist(By.className("time-select-panel"));

        assert getRecord().size() == 6;

        driver.findElementByTagName("lv-date-interval").click();

        JavascriptExecutor j = (JavascriptExecutor)driver;
        j.executeScript("arguments[0].value='2012-01-01 00:00';", timeSelectPanel().findElement(By.name("startDate")));

        assertThat(timeSelectPanel().findElement(By.name("startDate")).getAttribute("value"), is("2012-01-01 00:00"));

        timeSelectPanel().findElement(By.name("cancel-button")).click();

        notExist(By.className("time-select-panel"));

        assert getRecord().size() == 6;

        assertThat(dateFilterHeader(), is("Since 2012-01-01 00:39"));
    }

    @Test
    public void addFilterFromContextMenu() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));

        openLog("search.log");

        new Actions(driver).contextClick(recordByText("[2012.01.01 00:41][        ::] sss 41 41")).perform();

        driver.findElementByXPath("//ul[@class='dropdown-menu show']/li[contains(., 'older')]").click();

        assert getRecord().size() == 4;

        notExist(By.xpath("//ul[@class='dropdown-menu show']"));

        new Actions(driver).contextClick(recordByText("[2012.01.01 00:43][      ::::] sss 43 43")).perform();

        driver.findElementByXPath("//ul[@class='dropdown-menu show']/li[contains(., 'newer')]").click();

        assert getRecord().size() == 3;
        assertThat(dateFilterHeader(), is("2012-01-01 00:41 - 2012-01-01 00:43"));
    }

    @Test
    public void startMoreThanEnd() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setDate(new FilterPanelState.DateFilter()));
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}]%m"));

        openLog("search.log");

        driver.findElementByTagName("lv-date-interval").click();
        timeSelectPanel().findElement(By.name("startDate")).sendKeys("2012-05-05 00:39");
        timeSelectPanel().findElement(By.name("endDate")).sendKeys("2012-01-01 00:39");

        driver.findElementByClassName("time-range-error");

        timeSelectPanel().findElement(By.name("startDate")).sendKeys(Keys.ENTER);

        driver.findElementByClassName("time-range-error");

        timeSelectPanel().findElement(By.name("apply-button")).click();

        driver.findElementByClassName("time-range-error");

        timeSelectPanel().findElement(By.className("clear-icon")).click();

        notExist(By.className("time-range-error"));
    }

}