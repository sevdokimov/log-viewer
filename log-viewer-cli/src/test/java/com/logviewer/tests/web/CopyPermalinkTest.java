package com.logviewer.tests.web;

import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class CopyPermalinkTest extends AbstractWebTestCase {

    @Test
    public void openPermalink() throws IOException {
        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, IntStream.rangeClosed(1, 20).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        openLog(tmpLog);
        setHeight(5);
        driver.navigate().refresh();
        recordByText("20"); // wait for initialization

        new Actions(driver).sendKeys(Keys.HOME).perform();

        String link0 = copyPermalink();
        assert link0.contains("state=");

        new Actions(driver).sendKeys(Keys.DOWN, Keys.DOWN).perform();

        assert !recordByText("1").isDisplayed();
        assert recordByText("2").isDisplayed();

        WebElement r5 = recordByText("5");
        r5.click();
        assert r5.getAttribute("className").contains("selected-line");

        String link1 = copyPermalink();

        String url = driver.getCurrentUrl();

        driver.get(link0);
        assert recordByText("1").isDisplayed();
        assert recordByText("2").isDisplayed();

        assertEquals(url, driver.getCurrentUrl());

        notExist(By.cssSelector("#records > .selected-line"));

        driver.get(link1);
        assert !recordByText("1").isDisplayed();
        assert recordByText("2").isDisplayed();
        assertEquals("5", driver.findElement(By.cssSelector("#records > .selected-line")).getText());

        notExist(By.id("brokenLinkGoTail"));

        assert !driver.getCurrentUrl().contains("state=");
    }

    @Test
    public void openPermalinkWithHideUnmatched() throws IOException {
        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, IntStream.rangeClosed(1, 100).mapToObj(i -> i % 2 == 0 ? "a" : "b")
                .collect(Collectors.joining("\n", "aaa-start\n", "\nend"))
                .getBytes());

        openLog(tmpLog);
        setHeight(5);
        driver.navigate().refresh();

        WebElement filterInput = FilterPanel.INPUT.findElement(driver);
        filterInput.sendKeys("a");

        WebElement hideUnmatched = driver.findElement(FilterPanel.HIDE_UNMATCHED);
        hideUnmatched.click();

        checkLastRecord("a");

        assert getVisibleRecords().matches("[\na]+");

        String link0 = copyPermalink();

        driver.get(link0);

        checkLastRecord("a");
        assert getVisibleRecords().matches("[\na]+");

        new Actions(driver).sendKeys(Keys.HOME).perform();

        recordByText("aaa-start");
        assert getVisibleRecords().matches("aaa-start[\na]+");
    }

    @Test
    public void jsonFilterInUrl() {
        ThreadFilterTest.setFormat();

        String path = getDataFilePath("thread-filter-test.log");
        openUrl("log",
                "path", path,
                "filters", "{\"date\":{\"endDate\":\"01325361900000000000\",\"startDate\":\"01325361660000000000\"},\"thread\":{\"includes\":[\"exec-*\"]},\"textFilters\":[{\"id\":\"3050352091\",\"pattern\":{\"s\":\"[exec-100] c\"},\"exclude\":true}],\"jsFilters\":[]}");

        waitForRecordsLoading();

        assertEquals("[2012.01.01 00:01][exec-1] b\n[2012.01.01 00:03][exec-100] d", getVisibleRecords());

        String link = copyPermalink();
        assert !link.contains("filters=");

        driver.get(link);

        waitForRecordsLoading();
        assertEquals("[2012.01.01 00:01][exec-1] b\n[2012.01.01 00:03][exec-100] d", getVisibleRecords());
        assert driver.getCurrentUrl().contains("filters=%7B%22");
    }

    @Test
    public void brokenLink() throws IOException {
        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, IntStream.rangeClosed(1, 20).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        openLog(tmpLog);
        setHeight(5);
        driver.navigate().refresh();

        new Actions(driver).sendKeys(Keys.HOME).perform();

        String link = copyPermalink();
        Files.write(tmpLog, IntStream.rangeClosed(2, 19).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        driver.get(link);

        notExist(By.xpath("//div[@id='records']/div[@class='record'][text()='1']"));

        driver.findElement(By.id("brokenLinkGoTail"));

        assert driver.getCurrentUrl().contains("state=");

        driver.navigate().refresh();

        driver.findElement(By.id("brokenLinkGoTail")).click();

        checkLastRecord("19");

        assert !driver.getCurrentUrl().contains("state=");
    }

    @Test
    public void nonAppliedSearchPattern() {
        openLog("search.log");

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys("2012.01.01 00:03");

        WebElement hideUnmatched = driver.findElement(FilterPanel.HIDE_UNMATCHED);
        hideUnmatched.click();

        assertEquals("[2012.01.01 00:03][      aaaa] sss 3 3", getVisibleRecords());

        driver.findElement(By.cssSelector("#applySearchFilterBlock .tooliconDisabled"));

        filterInput.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE);

        notExist(By.cssSelector("#applySearchFilterBlock .tooliconDisabled"));

        String link = copyPermalink();

        driver.get(link);

        driver.findElement(By.cssSelector("#applySearchFilterBlock .tooliconDisabled"));
        filterInput = driver.findElement(FilterPanel.INPUT);
        assertEquals("2012.01.01 00:03", filterInput.getAttribute("value"));

        assertEquals("[2012.01.01 00:03][      aaaa] sss 3 3", getVisibleRecords());

        driver.findElement(By.cssSelector("#hide-unmatched:checked"));
        assertEquals("2012.01.01 00:03", join(driver.findElements(By.className("search-result"))));
    }

    @Test
    public void copyPermalinkAfterSearch() {
        openLog("search.log");
        setHeight(5);

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys("2012.01.01 00:03");
        new Actions(driver).keyDown(Keys.SHIFT).sendKeys(filterInput, Keys.F3).keyUp(Keys.SHIFT).perform();

        List<WebElement> searchRes = driver.findElements(By.className("search-result"));

        assertEquals("2012.01.01 00:03", join(searchRes));

        Point location = searchRes.get(0).getLocation();

        String link = copyPermalink();

        driver.get(link);

        searchRes = driver.findElements(By.className("search-result"));

        assertEquals("2012.01.01 00:03", join(searchRes));

        assertEquals(location, searchRes.get(0).getLocation());
    }

    @Test
    public void copyPermalinkLevelFilter() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new LogbackLogFormat("%d{yyMMdd HH:mm:ss} %p %m%n"));

        openLog("level-logback.log");

        driver.findElement(By.cssSelector("lv-level-list > div > span")).click();
        WebElement warn = driver.findElements(By.cssSelector(".level-drop-down .level-name")).stream().filter(r -> r.getText().equals("WARN")).findFirst().get();
        warn.click();

        waitFor(() -> {
            return getVisibleRecords().equals("150101 12:00:01 WARN www\n150101 12:00:01 ERROR eee");
        });

        String link = copyPermalink();

        driver.get(link);

        waitFor(() -> {
            return getVisibleRecords().equals("150101 12:00:01 WARN www\n150101 12:00:01 ERROR eee");
        });
    }
}
