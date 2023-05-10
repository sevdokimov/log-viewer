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

        driver.findElement(By.id("match-cases")).click();

        assertEquals("filterInput", driver.executeScript("return document.activeElement.id"));

        driver.findElement(By.id("match-cases")).click();

        assertEquals("filterInput", driver.executeScript("return document.activeElement.id"));
    }

    @Test
    public void upDownArrows() {
        setHeight(5);
        openLog("search.log");

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        WebElement prevArrow = driver.findElement(By.id("findPrevArrow"));
        WebElement nextArrow = driver.findElement(By.id("findNextArrow"));

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

        driver.findElement(By.id("match-regex")).click();

        assertEquals(":::::", join(driver.findElements(By.className("search-result"))));

        filterInput.sendKeys("\\");
        driver.findElement(By.cssSelector("#filterInput.search-invalid-regex"));
        assert filterInput.getAttribute("title").contains("\\") : filterInput.getAttribute("title");
        notExist(By.className("search-result"));

        assert driver.findElement(By.id("findPrevArrow")).getAttribute("class").contains("tooliconDisabled");
        assert driver.findElement(By.id("findNextArrow")).getAttribute("class").contains("tooliconDisabled");


        driver.findElement(By.id("match-regex")).click();
        waitFor(() -> !filterInput.getAttribute("class").contains("search-invalid-regex"));
        assertEquals("", filterInput.getAttribute("title"));
        notExist(By.className("search-result"));

        setValue(filterInput, "");
        filterInput.sendKeys("ss");

        driver.findElement(By.className("search-result"));
        filterInput.sendKeys("S");
        driver.findElement(By.className("search-result"));

        driver.findElement(By.id("match-cases")).click();

        notExist(By.className("search-result"));

        driver.findElement(By.id("match-cases")).click();

        filterInput.sendKeys("\\b");
        notExist(By.className("search-result"));
        driver.findElement(By.id("match-regex")).click();
        driver.findElement(By.className("search-result"));
        driver.findElement(By.id("match-cases")).click();
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

        assertEquals(" a]", join(driver.findElements(By.className("search-result"))));

        new Actions(driver).sendKeys(Keys.ESCAPE, Keys.END).perform();
        notExist(By.className("search-result"));

        clearInput(filterInput);
        filterInput.sendKeys(" A]");
        shiftF3(filterInput);

        assertEquals(" a]", join(driver.findElements(By.className("search-result"))));

        driver.findElement(By.id("match-cases")).click();
        shiftF3(filterInput);

        notExist(By.className("search-result"));

        driver.findElement(By.id("match-cases")).click();
        shiftF3(filterInput);

        assertEquals(" a]", join(driver.findElements(By.className("search-result"))));

        new Actions(driver).sendKeys(Keys.ESCAPE, Keys.ESCAPE, Keys.END).perform();
        notExistWait(By.className("search-result"));
        driver.findElement(By.id("match-regex")).click();

        setValue(filterInput, "");
        filterInput.sendKeys(" A\\]");
        shiftF3(filterInput);
        assertEquals(" a]", join(driver.findElements(By.className("search-result"))));

        new Actions(driver).sendKeys(Keys.ESCAPE, Keys.ESCAPE, Keys.END).perform();
        notExist(By.className("search-result"));
        driver.findElement(By.id("match-cases")).click();
        shiftF3(filterInput);

        closeInfoAlert();

        notExist(By.className("search-result"));

        setValue(filterInput, "");
        filterInput.sendKeys(" a\\]");
        shiftF3(filterInput);
        assertEquals(" a]", join(driver.findElements(By.className("search-result"))));
    }

    private void shiftF3(WebElement filterInput) {
        new Actions(driver).keyDown(Keys.SHIFT).sendKeys(filterInput, Keys.F3).keyUp(Keys.SHIFT).perform();
    }

    @Test
    public void hideUnmatchedEnableDisable() {
        openLog("search.log");

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys("::");  // ::

        driver.findElement(By.id("findPrevArrow"));
        driver.findElement(By.id("findNextArrow"));

        WebElement hideUnmatched = driver.findElement(FilterPanel.HIDE_UNMATCHED);

        Point labelLocation = hideUnmatched.getLocation();

        hideUnmatched.click();

        notExist(By.id("findPrevArrow"));
        notExist(By.id("findNextArrow"));
        assertEquals(labelLocation, hideUnmatched.getLocation());

        WebElement applySearchFilter = driver.findElement(By.id("applySearchFilter"));
        assert applySearchFilter.getAttribute("class").contains("tooliconDisabled");

        waitFor(() -> driver.findElements(By.cssSelector("#records .record")).size() == 4);

        assertEquals("::::::::::::", join(driver.findElements(By.className("search-result"))));

        filterInput.sendKeys(":"); // :::

        assertEquals("::::::::::::", join(driver.findElements(By.className("search-result"))));

        assert !applySearchFilter.getAttribute("class").contains("tooliconDisabled");
        filterInput.sendKeys(Keys.BACK_SPACE);  // ::
        assert applySearchFilter.getAttribute("class").contains("tooliconDisabled");

        filterInput.sendKeys(":"); // :::
        applySearchFilter.click();

        waitFor(() -> driver.findElements(By.cssSelector("#records .record")).size() == 3);
        assertEquals(":::::::::", join(driver.findElements(By.className("search-result"))));

        filterInput.sendKeys(":"); // ::::

        hideUnmatched.click();
        assertEquals(labelLocation, hideUnmatched.getLocation());

        waitFor(() -> driver.findElements(By.cssSelector("#records .record")).size() > 5);

        assertEquals("::::::::", join(driver.findElements(By.className("search-result"))));

        driver.findElement(By.id("findPrevArrow"));
        driver.findElement(By.id("findNextArrow"));
        notExist(By.id("applySearchFilter"));
    }

    @Test
    public void searchFieldSize() throws InterruptedException {
        openLog("search.log");

        WebElement filterDiv = driver.findElement(FilterPanel.INPUT).findElement(By.xpath("./.."));

        Dimension size = filterDiv.getSize();

        driver.findElement(By.id("match-cases")).click();
        Thread.sleep(10);

        assertEquals(size, filterDiv.getSize());

        driver.findElement(By.id("match-regex")).click();
        Thread.sleep(10);

        assertEquals(size, filterDiv.getSize());
    }
}