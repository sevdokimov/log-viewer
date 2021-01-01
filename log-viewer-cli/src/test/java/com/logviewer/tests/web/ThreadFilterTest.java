package com.logviewer.tests.web;

import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.utils.FilterPanelState;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ThreadFilterTest extends AbstractWebTestCase {

    @Test
    public void noFilterIfLogHasNoTimestamp() {
        openLog("1-7.log");

        driver.findElement(By.tagName("lv-top-filter"));

        new Actions(driver).contextClick(lastRecord()).perform();

        driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Event details')]"));
        notExist(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Thread')]"));
    }

    private String filterHeader() {
        return driver.findElement(By.cssSelector("lv-thread-filter .top-panel-dropdown > span")).getText().trim();
    }

    @Test
    public void initFilterExclude() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().excludeThreads("exec-*"));

        openLog("thread-filter-test.log");

        assertThat(getVisibleRecords(), is("[2012.01.01 00:04][main] d\n[2012.01.01 00:05][main] f"));

        assertThat(filterHeader(), is("- exec-*"));
    }

    @Test
    public void initFilterInclude() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().includeThreads("main"));

        openLog("thread-filter-test.log");

        assertThat(getVisibleRecords(), is("[2012.01.01 00:04][main] d\n[2012.01.01 00:05][main] f"));

        assertThat(filterHeader(), is("main"));
    }

    @Test
    public void initFilterIncludeExclude() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default",
                new FilterPanelState().includeThreads("exec-*").excludeThreads("exec-1"));

        openLog("thread-filter-test.log");

        assertThat(getVisibleRecords(), is("[2012.01.01 00:02][exec-100] c\n[2012.01.01 00:03][exec-100] d\n[2012.01.01 00:06][exec-100] g"));

        assertThat(filterHeader(), is("1 visible, 1 hidden threads"));
    }

    private void clearFilter(String filterType) {
        WebElement filterPanel = driver.findElement(By.tagName(filterType));
        new Actions(driver).moveToElement(filterPanel).perform();
        WebElement closeIcon = driver.findElement(By.cssSelector(filterType + " .closeable-filter > .remote-filter-icon"));
        closeIcon.click();
    }

    private void rightClickRecord(String recordText) {
        new Actions(driver).contextClick(recordByText(recordText)).perform();

        WebElement menuItem = driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Thread')]"));

        new Actions(driver).moveToElement(menuItem).perform();
    }

    @Test
    public void excludeFromContextMenu() {
        setFormat();

        openLog("thread-filter-test.log");

        assertThat(getRecord().size(), is(7));

        By submenu = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Hide exec-1')]");
        By submenuWildcard = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Hide exec-*')]");
        notExist(submenu);
        notExist(submenuWildcard);

        rightClickRecord("[2012.01.01 00:01][exec-1] b");

        // Exclude one thread
        driver.findElement(submenu).click();

        assertThat(filterHeader(), is("- exec-1"));
        assertThat(getRecord().size(), is(5));

        clearFilter("lv-thread-filter");

        notExist(By.tagName("lv-thread-filter"));
        assertThat(getRecord().size(), is(7));

        // Exclude one wildcard
        rightClickRecord("[2012.01.01 00:00][exec-1] a");
        driver.findElement(submenuWildcard).click();

        assertThat(filterHeader(), is("- exec-*"));
        assertThat(getRecord().size(), is(2));

        clearFilter("lv-thread-filter");
        notExist(By.tagName("lv-thread-filter"));
    }

    @Test
    public void includeFromContextMenu() {
        setFormat();

        openLog("thread-filter-test.log");

        assertThat(getRecord().size(), is(7));

        By submenu = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Show only exec-1')]");
        By submenuWildcard = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Show only exec-*')]");
        notExist(submenu);
        notExist(submenuWildcard);

        rightClickRecord("[2012.01.01 00:01][exec-1] b");

        // Exclude one thread
        driver.findElement(submenu).click();

        assertThat(filterHeader(), is("exec-1"));
        assertThat(getRecord().size(), is(2));

        clearFilter("lv-thread-filter");

        notExist(By.tagName("lv-thread-filter"));
        assertThat(getRecord().size(), is(7));

        // Exclude one wildcard
        rightClickRecord("[2012.01.01 00:00][exec-1] a");
        driver.findElement(submenuWildcard).click();

        assertThat(filterHeader(), is("exec-*"));
        assertThat(getRecord().size(), is(5));
    }

    @Test
    public void noWildcard() {
        setFormat();

        openLog("thread-filter-test.log");

        assertThat(getRecord().size(), is(7));

        By submenu = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Show only \"exec-1\"')]");
        By submenuWildcard = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Show only \"exec-*\"')]");
        notExist(submenu);
        notExist(submenuWildcard);

        rightClickRecord("[2012.01.01 00:04][main] d");

        WebElement show = driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Show only main')]"));
        WebElement hide = driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Hide main')]"));

        List<WebElement> allMenuItems = show.findElements(By.xpath("../*"));

        assertThat(allMenuItems, is(Arrays.asList(show, hide)));
    }

    @Test
    public void editThreads() {
        setFormat();

        ctx.getBean(TestFilterPanelState.class).addFilterSet("default",
                new FilterPanelState().includeThreads("exec-1").excludeThreads("exec-100"));

        openLog("thread-filter-test.log");

        assertThat(getRecord().size(), is(2));

        By dropDown = By.cssSelector("lv-thread-filter > .lv-dropdown-panel");
        notExist(dropDown);

        WebElement threadDropdownOpener = driver.findElement(By.cssSelector("lv-thread-filter .lv-dropdown-panel-holder > span"));
        threadDropdownOpener.click();

        By threadBlocks = By.cssSelector("lv-thread-filter .lv-dropdown-panel .thread-block");

        // Remove
        List<WebElement> threads = driver.findElements(threadBlocks);

        assertThat(threads.stream().map(WebElement::getText).collect(Collectors.joining(",")), is("exec-1,exec-100"));

        WebElement removeIcon = threads.get(0).findElement(By.className("remove-icon"));
        new Actions(driver).moveToElement(removeIcon).perform();
        removeIcon.click();

        notExist(dropDown);

        assertThat(filterHeader(), is("- exec-100"));
        assertThat(getRecord().size(), is(4));
    }

    public static void setFormat() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new Log4jLogFormat("[%d{yyyy.MM.dd HH:mm}][%t] %m"));
    }

}
