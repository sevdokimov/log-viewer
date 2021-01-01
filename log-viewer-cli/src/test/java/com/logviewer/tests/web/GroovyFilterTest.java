package com.logviewer.tests.web;

import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.utils.FilterPanelState.GroovyFilter;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.logviewer.tests.web.ThreadFilterTest.setFormat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class GroovyFilterTest extends AbstractWebTestCase {

    public static final By HEADERS = By.cssSelector("lv-groovy-filter .top-panel-dropdown");

    private List<String> filterHeaders() {
        return driver.findElements(HEADERS).stream()
                .map(e -> e.getText().trim())
                .collect(Collectors.toList());
    }

    @Test
    public void initFilterExclude() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().groovyFilter(
                new GroovyFilter("1", "", "   \n\n\t  thread   ==\t\t 'exec-100'"),
                new GroovyFilter("2", "                                                         always-  true", "true")
        ));

        openLog("thread-filter-test.log");

        assertThat(getRecord().size(), is(3));
        assertThat(filterHeaders(), is(Arrays.asList("Script: thread == 'exec-100'", "always- true")));
        assert getRecord().stream().map(WebElement::getText).allMatch(t -> t.contains("exec-100"));
    }

    @Test
    public void hugeTitles() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().groovyFilter(
                new GroovyFilter("1", "", "thread == 'exec-100 || false || false || false || false || false || false || false || false || false || false || false || false || false'"),
                new GroovyFilter("2", "always-true always-true always-true always-true always-true always-true always-true always-true always-true always-true always-true always-true ", "true")
        ));

        openLog("thread-filter-test.log");

        List<WebElement> headers = driver.findElements(HEADERS);
        assertThat(headers.size(), is(2));

        assert headers.get(0).getSize().width <= 400;
        assert headers.get(1).getSize().width <= 400;
    }

    @Test
    public void addingRemoving() {
        setFormat();

        openLog("thread-filter-test.log");

        notExist(HEADERS);

        addFilterMenuClick();

        driver.findElement(By.xpath("//div[@class='add-filter-menu']//div[contains(@class,'dropdown-menu')]//a[contains(text(),'Groovy')]")).click();

        assertThat(driver.findElements(HEADERS).size(), is(1));
        driver.findElement(By.cssSelector(".lv-dropdown-panel")); // Dropdown opened automatically

        addFilterMenuClick();
        driver.findElement(By.xpath("//div[@class='add-filter-menu']//div[contains(@class,'dropdown-menu')]//a[contains(text(),'Groovy')]")).click();
        addFilterMenuClick();
        driver.findElement(By.xpath("//div[@class='add-filter-menu']//div[contains(@class,'dropdown-menu')]//a[contains(text(),'Groovy')]")).click();

        List<WebElement> filters = driver.findElements(HEADERS);
        assertThat(filters.size(), is(3));
        assertThat(driver.findElements(By.cssSelector(".lv-dropdown-panel")).size(), is(1)); // One dropdown only is opened as the same time

        checkEditing(filters.get(2));

        WebElement closer = filters.get(1).findElement(By.cssSelector(".remote-filter-icon"));
        new Actions(driver).moveToElement(closer).click().perform();

        closer = filters.get(2).findElement(By.cssSelector(".remote-filter-icon"));
        new Actions(driver).moveToElement(closer).click().perform();

        assertThat(driver.findElements(HEADERS).size(), is(1));
    }

    private void checkEditing(WebElement filter) {
        driver.findElement(By.cssSelector(".lv-dropdown-panel"));

        // editing name
        WebElement nameInput = filter.findElement(By.cssSelector(".lv-dropdown-panel .filter-name input"));
        new Actions(driver).sendKeys(nameInput, "aaa").perform();

        assertThat(filter.getAttribute("class"), containsString("modified"));

        List<WebElement> buttons = filter.findElements(By.cssSelector(".action-panel button"));
        buttons.get(1).click();

        notExist(filter, By.className("lv-dropdown-panel"));
        assertThat(filter.getAttribute("class"), not(containsString("modified")));

        checkSaveOnButtonClick(filter);
        checkSaveOnEnter(filter);
    }

    private void checkSaveOnButtonClick(WebElement filter) {
        filter.click();
        WebElement nameInput = filter.findElement(By.cssSelector(".lv-dropdown-panel .filter-name input"));
        assertThat(nameInput.getAttribute("value"), is(""));

        new Actions(driver).sendKeys(nameInput, "aaa").perform();

        List<WebElement> buttons = filter.findElements(By.cssSelector(".action-panel button"));
        buttons.get(0).click(); // "Save" button

        assertThat(filter.getText().trim(), is("aaa"));
    }

    private void checkSaveOnEnter(WebElement filter) {
        filter.click();
        WebElement nameInput = filter.findElement(By.cssSelector(".lv-dropdown-panel .filter-name input"));
        setValue(nameInput, "");
        new Actions(driver).sendKeys(nameInput, "ttt", Keys.ENTER).perform();

        notExist(filter, By.className("lv-dropdown-panel"));
        assertThat(filter.getText().trim(), is("ttt"));

    }

    private void addFilterMenuClick() {
        driver.findElement(By.cssSelector(".add-filter-menu .add-filter-button")).click();
    }

}
