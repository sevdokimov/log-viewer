package com.logviewer.tests.web;

import com.google.common.collect.Iterables;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.web.session.tasks.SearchPattern;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.logviewer.tests.web.ThreadFilterTest.setFormat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class TextFilterTest extends AbstractWebTestCase {

    public static final By HEADERS = By.cssSelector("lv-text-filter .top-panel-dropdown");
    public static final By TEXTAREA = By.tagName("textarea");
    public static final By APPLY = By.cssSelector(".action-panel button[name=\"apply-button\"]");
    public static final By ADD_TEXT_FILTER = By.xpath("//div[@class='add-filter-menu']//div[contains(@class,'dropdown-menu')]//a[contains(text(),'Text')]");
    public static final By MENU = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Text:')]");
    public static final By TEXT_AREA_REGEX = By.cssSelector("textarea.regex-mode");
    public static final By ONLY_EVENT_CONTAINIG = By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Only events containing')]");

    @Test
    public void hugeTitles() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().textFilter(
                new FilterPanelState.TextFilter("1", "",
                        new SearchPattern("thread == 'exec-100 || false || false || false || false || false || false || false || false || false || false || false || false || false'")
                        , true)
        ));

        openLog("thread-filter-test.log");

        List<WebElement> headers = driver.findElements(HEADERS);
        assertThat(headers.size(), is(1));

        assert headers.get(0).getSize().width <= 400;
    }

    @Test
    public void enablingDisabling() {
        setFormat();
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().textFilter(
                new FilterPanelState.TextFilter("1", "", new SearchPattern("exec-100"), false).disable()
        ));

        openLog("thread-filter-test.log");

        checkRecordCount(7);

        WebElement panel = Iterables.getOnlyElement(driver.findElements(HEADERS));

        panel.findElement(By.cssSelector(".top-panel-dropdown > span.disabled-filter")); // title has "disabled-filter" class

        panel.findElement(By.cssSelector(".top-panel-dropdown > span")).click();

        panel.findElement(By.tagName("lv-switch")).click();

        checkRecordCount(3);

        WebElement textArea = panel.findElement(By.cssSelector("textarea"));

        setValue(textArea, "");
        new Actions(driver).sendKeys(textArea, "[main]").perform();

        panel.findElement(By.tagName("lv-switch")).click();

        checkRecordCount(7);

        assertThat(textArea.getAttribute("value"), is("[main]"));

        driver.navigate().refresh();

        checkRecordCount(7);

        panel = Iterables.getOnlyElement(driver.findElements(HEADERS));
        panel.findElement(By.cssSelector(".top-panel-dropdown > span.disabled-filter")).click();

        panel.findElement(By.tagName("lv-switch")).click();

        checkRecordCount(2);
    }

    @Test
    public void addingRemoving() {
        setFormat();

        openLog("thread-filter-test.log");

        notExist(HEADERS);

        addFilterMenuClick();

        driver.findElement(ADD_TEXT_FILTER).click();

        assertThat(driver.findElements(HEADERS).size(), is(1));
        driver.findElement(By.cssSelector("lv-text-filter .lv-dropdown-panel textarea:focus")); // Dropdown opened automatically

        new Actions(driver).sendKeys("exec-100").perform();

        WebElement filter = driver.findElement(HEADERS);

        filter.findElement(APPLY).click();

        notExist(By.cssSelector("lv-text-filter .lv-dropdown-panel"));
        checkRecordCount(3);

        checkSaveOnEnter(filter);

        growAndRevert(filter);

        addFilterMenuClick();
        driver.findElement(By.xpath("//div[@class='add-filter-menu']//div[contains(@class,'dropdown-menu')]//a[contains(text(),'Text')]")).click();

        assertThat(driver.findElements(HEADERS).size(), is(2));

        driver.findElements(HEADERS).get(1).click(); // close dropdown

        WebElement closer = filter.findElement(By.cssSelector(".remote-filter-icon"));
        new Actions(driver).moveToElement(closer).click().perform();

        assertThat(driver.findElements(HEADERS).size(), is(1));

        closer = driver.findElement(HEADERS).findElement(By.cssSelector(".remote-filter-icon"));
        new Actions(driver).moveToElement(closer).click().perform();

        notExist(HEADERS);
    }

    private void growAndRevert(WebElement filter) {
        filter.click();

        WebElement textArea = filter.findElement(By.cssSelector("textarea"));
        Dimension size = textArea.getSize();

        assert size.height > 10 && size.height < 100;

        String initValue = textArea.getAttribute("value");

        setValue(textArea, "");
        new Actions(driver).sendKeys(String.join("\n", Collections.nCopies(50, "aaa"))).perform();

        Dimension maxSize = textArea.getSize();

        assert size.height < maxSize.height;

        setValue(textArea, "");
        new Actions(driver).sendKeys(String.join("\n", Collections.nCopies(50, "aaa"))).perform();

        assert maxSize.height == textArea.getSize().height; // height is not more than max size

        filter.findElement(By.cssSelector(".action-panel button[name=\"cancel-button\"]")).click();

        notExist(By.cssSelector("lv-text-filter .lv-dropdown-panel"));

        filter.click();

        textArea = filter.findElement(By.cssSelector("textarea"));

        assertThat(textArea.getAttribute("value"), is(initValue));

        filter.findElement(By.cssSelector(".action-panel button[name=\"cancel-button\"]")).click();
    }

    @Test
    public void checkMatchCase() {
        setFormat();
        openLog("thread-filter-test.log");

        notExist(HEADERS);

        addFilterMenuClick();
        driver.findElement(ADD_TEXT_FILTER).click();

        WebElement filter = driver.findElement(HEADERS);

        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "EXEC-100").perform();
        filter.findElement(APPLY).click();

        checkRecordCount(3);

        filter.click();

        WebElement matchCaseCheckbox = filter.findElement(By.name("match-case-checkbox"));
        matchCaseCheckbox.click();

        filter.findElement(APPLY).click();

        checkRecordCount(0);

        filter.click();

        matchCaseCheckbox = filter.findElement(By.name("match-case-checkbox"));
        assert !matchCaseCheckbox.getAttribute("checked").equals("");

        matchCaseCheckbox.click();
        filter.findElement(APPLY).click();
        checkRecordCount(3);
    }

    @Test
    public void checkIncludeExclude() {
        setFormat();
        openLog("thread-filter-test.log");

        notExist(HEADERS);

        addFilterMenuClick();
        driver.findElement(ADD_TEXT_FILTER).click();

        WebElement filter = driver.findElement(HEADERS);

        filter.findElement(By.xpath("//div[@class='include-exclude-group']/label[contains(normalize-space(.),'Show')]"));

        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "exec-").perform();

        filter.findElement(APPLY).click();
        
        checkRecordCount(5);

        filter.click();

        filter.findElement(By.xpath("//div[@class='include-exclude-group']/label[contains(normalize-space(.),'Hide')]")).click();

        filter.findElement(APPLY).click();

        checkRecordCount(2);
    }

    @Test
    public void checkRegex() {
        setFormat();
        openLog("thread-filter-test.log");

        notExist(HEADERS);

        addFilterMenuClick();
        driver.findElement(ADD_TEXT_FILTER).click();

        WebElement filter = driver.findElement(HEADERS);

        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "EXEC-\\d+").perform();
        filter.findElement(APPLY).click();
        checkRecordCount(0);

        filter.click();
        setValue(filter.findElement(TEXTAREA), "EXEC-\\d+");
        notExist(filter, TEXT_AREA_REGEX);
        filter.findElement(By.name("regex-checkbox")).click();
        filter.findElement(TEXT_AREA_REGEX);

        filter.findElement(APPLY).click();
        checkRecordCount(5);

        filter.click();
        setValue(filter.findElement(TEXTAREA), "EXEC-\\d+");
        filter.findElement(By.name("match-case-checkbox")).click();
        filter.findElement(APPLY).click();
        checkRecordCount(0);
    }

    @Test
    public void regexpError() {
        setFormat();
        openLog("thread-filter-test.log");

        notExist(HEADERS);

        addFilterMenuClick();
        driver.findElement(ADD_TEXT_FILTER).click();

        WebElement filter = driver.findElement(HEADERS);

        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "aaa").perform();
        filter.findElement(By.name("regex-checkbox")).click();

        assert filter.findElement(By.cssSelector(".regexp-error")).getText().trim().isEmpty();

        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "(").perform();

        assert !filter.findElement(By.cssSelector(".regexp-error")).getText().trim().isEmpty();

        filter.findElement(By.name("regex-checkbox")).click();

        assert filter.findElement(By.cssSelector(".regexp-error")).getText().trim().isEmpty();

        filter.findElement(By.name("regex-checkbox")).click();

        assert !filter.findElement(By.cssSelector(".regexp-error")).getText().trim().isEmpty();

        setValue(filter.findElement(TEXTAREA), "");
        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "()").perform();

        waitFor(() -> filter.findElement(By.cssSelector(".regexp-error")).getText().trim().isEmpty());
        assertThat(filter.findElement(APPLY).getAttribute("disabled"), CoreMatchers.nullValue());

        new Actions(driver).sendKeys(filter.findElement(TEXTAREA), "(").perform();

        assert !filter.findElement(By.cssSelector(".regexp-error")).getText().trim().isEmpty();

        assertThat(filter.findElement(APPLY).getAttribute("disabled"), is("true"));
    }

    @Test
    public void selectionFoldedElement() throws IOException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(new LogbackLogFormat("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %level %logger %msg%n"));

        String logPath = openLog("rendering/strange-exception-line.log");

        String text = new String(Files.readAllBytes(Paths.get(logPath)));

        select(lastRecord().findElement(By.cssSelector(".rec-text")));
        new Actions(driver).contextClick(lastRecord()).perform();

        new Actions(driver).moveToElement(driver.findElement(MENU)).perform();

        driver.findElement(ONLY_EVENT_CONTAINIG).click();

        checkRecordCount(1);

        WebElement filter = driver.findElement(HEADERS);
        filter.click();
        String textFilterValue = filter.findElement(TEXTAREA).getAttribute("value");

        assertEquals(text, textFilterValue);
    }

    @Test
    public void excludeIncludeFromContextMenu() {
        setFormat();

        openLog("thread-filter-test.log");

        assertThat(getRecord().size(), is(7));

        WebElement record = recordByText("[2012.01.01 00:01][exec-1] b").findElement(By.cssSelector(".rec-text"));

        new Actions(driver).contextClick(record).perform();
        notExist(ONLY_EVENT_CONTAINIG); // no selection - no "Only events containing" menu item
        new Actions(driver).sendKeys(Keys.ESCAPE).perform(); // close context menu

        select(record);

        new Actions(driver).contextClick(record).perform();

        new Actions(driver).moveToElement(driver.findElement(MENU)).perform();

        driver.findElement(ONLY_EVENT_CONTAINIG).click();

        notExist(MENU);

        checkRecordCount(1);

        WebElement closer = driver.findElement(By.cssSelector("lv-text-filter .remote-filter-icon"));
        new Actions(driver).moveToElement(closer).click().perform();

        checkRecordCount(7);

        select(record);

        new Actions(driver).contextClick(record).perform();

        new Actions(driver).moveToElement(driver.findElement(MENU)).perform();

        driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li[contains(., 'Hide events containing')]")).click();

        notExist(MENU);

        checkRecordCount(6);
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
