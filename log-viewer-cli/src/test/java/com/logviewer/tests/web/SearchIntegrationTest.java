package com.logviewer.tests.web;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SearchIntegrationTest extends AbstractWebTestCase {

    @Test
    public void highlightOnType() {
        openLog("search.log");
        setHeight(5);
        driver.navigate().refresh();

        WebElement recordsParent = driver.findElement(By.id("records"));

        List<WebElement> records = recordsParent.findElements(By.cssSelector(".record"));
        assertEquals(12, records.size());

        driver.findElement(FilterPanel.INPUT).click();

        WebElement filterInput = driver.findElement(By.cssSelector("#filterInput:focus"));

        filterInput.sendKeys("2012.01.01 00:");

        List<WebElement> searchResults = recordsParent.findElements(By.className("search-result"));
        assertEquals(Joiner.on("").join(Collections.nCopies(12, "2012.01.01 00:")), join(searchResults));

        new Actions(driver).sendKeys("4").perform();

        searchResults = recordsParent.findElements(By.className("search-result"));
        assertEquals(Joiner.on("").join(Collections.nCopies(5, "2012.01.01 00:4")), join(searchResults));

        new Actions(driver).sendKeys(Keys.BACK_SPACE).perform();

        searchResults = recordsParent.findElements(By.className("search-result"));
        assertEquals(Joiner.on("").join(Collections.nCopies(12, "2012.01.01 00:")), join(searchResults));
    }

    @Test
    public void focusOnFlagsChanging() {
        openLog("search.log");

        assertFalse(driver.executeScript("return document.activeElement.id").equals("filterInput"));

        driver.findElementById("match-cases").click();

        assertEquals("filterInput", driver.executeScript("return document.activeElement.id"));

        driver.findElementById("match-cases").click();

        assertEquals("filterInput", driver.executeScript("return document.activeElement.id"));
    }

    @Test
    public void upDownArrows() {
        setHeight(5);
        openLog("search.log");

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        WebElement prevArrow = driver.findElementById("findPrevArrow");
        WebElement nextArrow = driver.findElementById("findNextArrow");

        assert prevArrow.getAttribute("class").contains("tooliconDisabled");
        assert nextArrow.getAttribute("class").contains("tooliconDisabled");

        filterInput.sendKeys("aaa");

        assert !prevArrow.getAttribute("class").contains("tooliconDisabled");
        assert !nextArrow.getAttribute("class").contains("tooliconDisabled");

        notExist(By.className("search-result"));
        prevArrow.click();
        driver.findElement(By.className("search-result"));

        clearInput(filterInput);

        assert prevArrow.getAttribute("class").contains("tooliconDisabled");
        assert nextArrow.getAttribute("class").contains("tooliconDisabled");
    }

    private void clearInput(WebElement filterInput) {
        new Actions(driver).keyDown(Keys.CONTROL).sendKeys(filterInput, "a").keyUp(Keys.CONTROL).sendKeys(Keys.DELETE).perform();
    }

    @Test
    public void searchFlags() throws InterruptedException {
        openLog("search.log");

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys(":{5,}");

        Thread.sleep(200);

        assert !filterInput.getAttribute("class").contains("search-invalid-regex");

        notExist(By.className("search-result"));

        driver.findElementById("match-regex").click();

        assertEquals(":::::", join(driver.findElements(By.className("search-result"))));

        filterInput.sendKeys("\\");
        driver.findElement(By.cssSelector("#filterInput.search-invalid-regex"));
        assert filterInput.getAttribute("title").contains("\\") : filterInput.getAttribute("title");
        notExist(By.className("search-result"));

        assert driver.findElementById("findPrevArrow").getAttribute("class").contains("tooliconDisabled");
        assert driver.findElementById("findNextArrow").getAttribute("class").contains("tooliconDisabled");


        driver.findElementById("match-regex").click();
        waitFor(() -> !filterInput.getAttribute("class").contains("search-invalid-regex"));
        assertEquals("", filterInput.getAttribute("title"));
        notExist(By.className("search-result"));

        setValue(filterInput, "");
        filterInput.sendKeys("ss");

        driver.findElement(By.className("search-result"));
        filterInput.sendKeys("S");
        driver.findElement(By.className("search-result"));

        driver.findElementById("match-cases").click();

        notExist(By.className("search-result"));

        driver.findElementById("match-cases").click();

        filterInput.sendKeys("\\b");
        notExist(By.className("search-result"));
        driver.findElementById("match-regex").click();
        driver.findElement(By.className("search-result"));
        driver.findElementById("match-cases").click();
        notExist(By.className("search-result"));
    }

    @Test
    public void searchLong() {
        openLog("search.log");
        setHeight(5);
        driver.navigate().refresh();

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys(" a]");
        shiftF3(filterInput);

        assertEquals(" a]", join(driver.findElementsByClassName("search-result")));

        new Actions(driver).sendKeys(Keys.ESCAPE, Keys.END).perform();
        notExist(By.className("search-result"));

        clearInput(filterInput);
        filterInput.sendKeys(" A]");
        shiftF3(filterInput);

        assertEquals(" a]", join(driver.findElementsByClassName("search-result")));

        driver.findElementById("match-cases").click();
        shiftF3(filterInput);

        notExist(By.className("search-result"));

        driver.findElementById("match-cases").click();
        shiftF3(filterInput);

        assertEquals(" a]", join(driver.findElementsByClassName("search-result")));

        new Actions(driver).sendKeys(Keys.ESCAPE, Keys.ESCAPE, Keys.END).perform();
        notExistWait(By.className("search-result"));
        driver.findElementById("match-regex").click();

        setValue(filterInput, "");
        filterInput.sendKeys(" A\\]");
        shiftF3(filterInput);
        assertEquals(" a]", join(driver.findElementsByClassName("search-result")));

        new Actions(driver).sendKeys(Keys.ESCAPE, Keys.ESCAPE, Keys.END).perform();
        notExist(By.className("search-result"));
        driver.findElementById("match-cases").click();
        shiftF3(filterInput);

        closeInfoAlert();

        notExist(By.className("search-result"));

        setValue(filterInput, "");
        filterInput.sendKeys(" a\\]");
        shiftF3(filterInput);
        assertEquals(" a]", join(driver.findElementsByClassName("search-result")));
    }

    private void shiftF3(WebElement filterInput) {
        new Actions(driver).keyDown(Keys.SHIFT).sendKeys(filterInput, Keys.F3).keyUp(Keys.SHIFT).perform();
    }

    @Test
    public void hideUnmatchedEnableDisable() {
        openLog("search.log");

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys("::");  // ::

        driver.findElementById("findPrevArrow");
        driver.findElementById("findNextArrow");

        WebElement hideUnmatched = driver.findElement(FilterPanel.HIDE_UNMATCHED);

        Point labelLocation = hideUnmatched.getLocation();

        hideUnmatched.click();

        notExist(By.id("findPrevArrow"));
        notExist(By.id("findNextArrow"));
        assertEquals(labelLocation, hideUnmatched.getLocation());

        WebElement applySearchFilter = driver.findElementById("applySearchFilter");
        assert applySearchFilter.getAttribute("class").contains("tooliconDisabled");

        waitFor(() -> driver.findElementsByCssSelector("#records .record").size() == 4);

        assertEquals("::::::::::::", join(driver.findElementsByClassName("search-result")));

        filterInput.sendKeys(":"); // :::

        assertEquals("::::::::::::", join(driver.findElementsByClassName("search-result")));

        assert !applySearchFilter.getAttribute("class").contains("tooliconDisabled");
        filterInput.sendKeys(Keys.BACK_SPACE);  // ::
        assert applySearchFilter.getAttribute("class").contains("tooliconDisabled");

        filterInput.sendKeys(":"); // :::
        applySearchFilter.click();

        waitFor(() -> driver.findElementsByCssSelector("#records .record").size() == 3);
        assertEquals(":::::::::", join(driver.findElementsByClassName("search-result")));

        filterInput.sendKeys(":"); // ::::

        hideUnmatched.click();
        assertEquals(labelLocation, hideUnmatched.getLocation());

        waitFor(() -> driver.findElementsByCssSelector("#records .record").size() > 5);

        assertEquals("::::::::", join(driver.findElementsByClassName("search-result")));

        driver.findElementById("findPrevArrow");
        driver.findElementById("findNextArrow");
        notExist(By.id("applySearchFilter"));
    }

    @Test
    public void searchFieldSize() throws InterruptedException {
        openLog("search.log");

        WebElement filterDiv = driver.findElement(FilterPanel.INPUT).findElement(By.xpath("./.."));

        Dimension size = filterDiv.getSize();

        driver.findElementById("match-cases").click();
        Thread.sleep(10);

        assertEquals(size, filterDiv.getSize());

        driver.findElementById("match-regex").click();
        Thread.sleep(10);

        assertEquals(size, filterDiv.getSize());
    }
}