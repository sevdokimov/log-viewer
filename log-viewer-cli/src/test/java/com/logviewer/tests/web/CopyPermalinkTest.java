package com.logviewer.tests.web;

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

        driver.get(link0);
        assert recordByText("1").isDisplayed();
        assert recordByText("2").isDisplayed();

        notExist(By.cssSelector("#records > .selected-line"));

        driver.get(link1);
        assert !recordByText("1").isDisplayed();
        assert recordByText("2").isDisplayed();
        assertEquals("5", driver.findElementByCssSelector("#records > .selected-line").getText());

        notExist(By.id("brokenLinkGoTail"));

        assert !driver.getCurrentUrl().contains("state=");
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

        driver.findElementById("brokenLinkGoTail");

        assert driver.getCurrentUrl().contains("state=");

        driver.navigate().refresh();

        driver.findElementById("brokenLinkGoTail").click();

        checkLastRecord("19");

        assert !driver.getCurrentUrl().contains("state=");
    }

    @Test
    public void nonAppliedSearchPattern() {
        openLog("search.log");

        WebElement filterInput = driver.findElementById("filterInput");
        filterInput.sendKeys("2012.01.01 00:03");

        WebElement hideUnmatched = driver.findElementById("hide-unmatched");
        hideUnmatched.click();

        assertEquals("[2012.01.01 00:03][      aaaa] sss 3 3", getVisibleRecords());

        driver.findElementByCssSelector("#applySearchFilterBlock .tooliconDisabled");

        filterInput.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE);

        notExist(By.cssSelector("#applySearchFilterBlock .tooliconDisabled"));

        String link = copyPermalink();

        driver.get(link);

        driver.findElementByCssSelector("#applySearchFilterBlock .tooliconDisabled");
        filterInput = driver.findElementById("filterInput");
        assertEquals("2012.01.01 00:03", filterInput.getAttribute("value"));

        assertEquals("[2012.01.01 00:03][      aaaa] sss 3 3", getVisibleRecords());

        driver.findElementByCssSelector("#hide-unmatched:checked");
        assertEquals("2012.01.01 00:03", join(driver.findElementsByClassName("search-result")));
    }

    @Test
    public void copyPermalinkAfterSearch() {
        openLog("search.log");
        setHeight(5);

        WebElement filterInput = driver.findElementById("filterInput");
        filterInput.sendKeys("2012.01.01 00:03");
        filterInput.sendKeys(Keys.chord(Keys.SHIFT, Keys.F3));

        List<WebElement> searchRes = driver.findElementsByClassName("search-result");

        assertEquals("2012.01.01 00:03", join(searchRes));

        Point location = searchRes.get(0).getLocation();

        String link = copyPermalink();

        driver.get(link);

        searchRes = driver.findElementsByClassName("search-result");

        assertEquals("2012.01.01 00:03", join(searchRes));

        assertEquals(location, searchRes.get(0).getLocation());
    }
}
