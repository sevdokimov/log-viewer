package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.Filter;
import com.logviewer.data2.LogFormat;
import com.logviewer.domain.FilterPanelState;
import com.logviewer.filters.FieldArgPredicate;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.mocks.InmemoryFilterStorage;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.utils.WebTestUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class FilterChangingTest extends AbstractWebTestCase {

    private static final LogFormat LOG_FORMAT = new RegexLogFormat(StandardCharsets.UTF_8,
            "(?<date>\\d{6} \\d\\d:\\d\\d:\\d\\d) (?<level>\\w+) (?<msg>.*)", true,
            "yyMMdd HH:mm:ss", "date",
            RegexLogFormat.field("date", FieldTypes.DATE),
            RegexLogFormat.field("level", FieldTypes.LEVEL_LOGBACK),
            RegexLogFormat.field("msg", "message")
    );

    @Test
    public void disableEnableSmallLog() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setNamedFilters(
                new Filter(null, false, TestPredicates.recordValues("7", "5"))));

        openLog("1-7.log");

        driver.executeScript("arguments[0].testAttr = '333'", recordByText("6"));

        assertEquals(7, driver.findElementsByCssSelector("#records > .record").size());

        filterState(0, true);
        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() == 5);

        // test that element was not rerendered
        assertEquals("333", driver.executeScript("return arguments[0].testAttr", recordByText("6")));

        assertEquals("1\n2\n3\n4\n6", driver.findElementsByCssSelector("#records > .record").stream()
                .map(WebElement::getText).collect(Collectors.joining("\n")));

        filterState(0, false);

        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() == 7);

        // test that element was not rerendered
        assertEquals("333", driver.executeScript("return arguments[0].testAttr", recordByText("6")));

        assertEquals("1\n2\n3\n4\n5\n6\n7", join(driver.findElementsByCssSelector("#records > .record"), "\n"));
        checkRecordViewPosition(recordByText("1"), 0);
    }

    @Test
    public void disableEnableLookAtTail() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setNamedFilters(
                new Filter(null, false, TestPredicates.ODD),
                new Filter(null, false, TestPredicates.recordValues("100"))));

        openLog("1-100.log");
        setHeight(5);
        new Actions(driver).sendKeys(Keys.END).perform();

        waitFor(() -> getRecordViewPosition(recordByText("97")) == 0);
        filterState(0, true);

        waitFor(() -> getRecordViewPosition(recordByText("94")) == 0);
        assertEquals("100", lastRecord().getText());

        filterState(0, false);
        waitFor(() -> getRecordViewPosition(recordByText("97")) == 0);
        assertEquals(LINE_HEIGHT * 3, getRecordViewPosition(recordByText("100")));
        assertEquals("100", lastRecord().getText());

        filterState(1, true);
        waitFor(() -> "99".equals(lastRecord().getText()));
        assertEquals(LINE_HEIGHT * 3, getRecordViewPosition(lastRecord()));

        filterState(1, false);
        waitFor(() -> "100".equals(lastRecord().getText()));
        assertEquals(LINE_HEIGHT * 3, getRecordViewPosition(lastRecord()));
    }

    @Test
    public void disableEnableLookAtTailSelected() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setNamedFilters(
                new Filter(null, false, TestPredicates.recordValues("99")),
                new Filter(null, false, TestPredicates.recordValues("97"))));

        openLog("1-100.log");
        setHeight(5);
        new Actions(driver).sendKeys(Keys.END).perform();

        recordByText("98").click();
        WebElement selected = driver.findElementByClassName("selected-line");
        assertEquals("98", selected.getText());

        assertEquals("97\n98\n99\n100", getVisibleRecords());

        int offset = getRecordViewPosition(selected);

        filterState(0, true);

        waitFor(() -> "97\n98\n100".equals(getVisibleRecords()));
        assertEquals(LINE_HEIGHT * 2, getRecordViewPosition(recordByText("100")));

        assertEquals(offset, getRecordViewPosition(selected));

        filterState(1, true);
        waitFor(() -> "96\n98\n100".equals(getVisibleRecords()));
        assertEquals(offset, getRecordViewPosition(selected));

        filterState(0, false);
        waitFor(() -> "96\n98\n99\n100".equals(getVisibleRecords()));
        assertEquals(offset, getRecordViewPosition(selected));
        waitFor(() -> getRecordViewPosition(recordByText("100")) == LINE_HEIGHT * 3);

        filterState(1, false);
        waitFor(() -> "97\n98\n99\n100".equals(getVisibleRecords()));
    }

    @Test
    public void searchOnAppearedLines() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState()
                .setNamedFilters(new Filter(null, true, new FieldArgPredicate("_", "zzz", FieldArgPredicate.Operator.NOT_EQUALS))));

        openLog("1-7.log");

        driver.findElementByCssSelector(".empty-log-message");
        driver.findElementById("filterInput").sendKeys("7");
        notExist(By.className("search-result"));

        filterState(0, false);

        assertEquals("7", driver.findElement(By.className("search-result")).getText());
    }

    @Test
    public void testUrlFilterState() throws InterruptedException {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setNamedFilters(
                new Filter(null, false, new FieldArgPredicate("_", "1", FieldArgPredicate.Operator.CONTAINS)),
                new Filter(null, true, new FieldArgPredicate("_", "7", FieldArgPredicate.Operator.CONTAINS))));

        openLog("1-7.log");

        assertEquals("123456", join(driver.findElementsByClassName("record")));

        assert !driver.getCurrentUrl().contains("filterSetName=");
        assert !driver.getCurrentUrl().contains("filters=");

        filterState(0, true);

        waitFor(() -> "23456".equals(join(driver.findElementsByClassName("record"))));

        assert !driver.getCurrentUrl().contains("filterSetName=");
        assert driver.getCurrentUrl().contains("filters=");

        driver.navigate().refresh();

        waitFor(() -> "23456".equals(join(driver.findElementsByClassName("record"))));

        filterState(0, false);

        assert !driver.getCurrentUrl().contains("filterSetName=");
        assert !driver.getCurrentUrl().contains("filters=");

        waitFor(() -> "123456".equals(join(driver.findElementsByClassName("record"))));

        driver.navigate().refresh();
        waitFor(() -> "123456".equals(join(driver.findElementsByClassName("record"))));
    }

    @Test
    public void invalidFilterState() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", "[1, 2, 3]");

        openLog("1-7.log");

        assertEquals(7, driver.findElementsByCssSelector("#records > .record").size()); // View is opened, no errors.

        ctx.getBean(TestFilterPanelState.class).addFilterSet("some-view-name", "{");

        openLog("1-7.log");

        assertEquals(7, driver.findElementsByCssSelector("#records > .record").size()); // View is opened, no errors.
    }

    @Test
    public void initFromBackendState() {
        FilterPanelState initState = new FilterPanelState().setLevel("ERROR", "DEBUG", "UNEXISTING_LEVEL"); // all levels
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", initState);

        ctx.getBean(TestFormatRecognizer.class).setFormat(LOG_FORMAT);

        openLog("level-logback.log");

        waitFor(() -> getVisibleRecords().matches(".+DEBUG.+\n.+ERROR.+"));

        assert !driver.getCurrentUrl().contains("filters=");

        WebElement editorHeader = driver.findElementByCssSelector("lv-level-list > div > span");

        assert editorHeader.getText().contains("DEBUG");
        assert editorHeader.getText().contains("*");

        editorHeader.click();

        List<WebElement> inputs = driver.findElementsByCssSelector(".level-drop-down > div > input");

        assertEquals("true", inputs.get(0).getAttribute("checked")); // ERROR
        assertNull(inputs.get(1).getAttribute("checked")); // WARN
        assertNull(inputs.get(2).getAttribute("checked")); // INFO
        assertEquals("true", inputs.get(3).getAttribute("checked")); // DEBUG
        assertNull(inputs.get(4).getAttribute("checked")); // TRACE

        inputs.get(2).click();

        waitFor(() -> getVisibleRecords().matches(".+DEBUG.+\n.+INFO.+\n.+ERROR.+"));

        assert driver.getCurrentUrl().contains("filters=");

        inputs = driver.findElementsByCssSelector(".level-drop-down > div > input");

        assertEquals("true", inputs.get(0).getAttribute("checked")); // ERROR
        assertNull(inputs.get(1).getAttribute("checked")); // WARN
        assertEquals("true", inputs.get(2).getAttribute("checked")); // INFO
        assertEquals("true", inputs.get(3).getAttribute("checked")); // DEBUG
        assertNull(inputs.get(4).getAttribute("checked")); // TRACE

        inputs.get(2).click();

        waitFor(() -> getVisibleRecords().matches(".+DEBUG.+\n.+ERROR.+"));

        assert !driver.getCurrentUrl().contains("filters=");
    }

    @Test
    public void levelTestInitState1() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(LOG_FORMAT);

        FilterPanelState initState = new FilterPanelState().setLevel("ERROR", "DEBUG", "WARN", "INFO", "TRACE", "UNEXISTING_LEVEL"); // all levels
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", initState);

        checkLevelEditorAllSelectedByDefault();

        Map<String, String> allFilters = ctx.getBean(InmemoryFilterStorage.class).getAllFilters();
        for (String value : allFilters.values()) {
            assert value.contains("UNEXISTING_LEVEL") : "Filter editor must not remove unknown values";
        }
    }

    @Test
    public void levelTestInitState2() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(LOG_FORMAT);

        FilterPanelState initState = new FilterPanelState().setLevel(); // all empty array
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", initState);

        checkLevelEditorAllSelectedByDefault();
    }

    @Test
    public void levelTestInitState3() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(LOG_FORMAT);

        FilterPanelState initState = new FilterPanelState(); // no "level" config
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", initState);

        checkLevelEditorAllSelectedByDefault();
    }

    private void checkLevelEditorAllSelectedByDefault() {
        openLog("level-logback.log");

        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() == 5);

        assert !driver.getCurrentUrl().contains("filters=");

        WebElement editorHeader = driver.findElementByCssSelector("lv-level-list > div > span");

        assert editorHeader.getText().contains("TRACE");
        assert !editorHeader.getText().contains("*");

        editorHeader.click();

        List<WebElement> inputs = driver.findElementsByCssSelector(".level-drop-down > div > input");
        assertEquals(5, inputs.size());
        inputs.forEach(i -> assertEquals("true", i.getAttribute("checked")));

        // Click to INFO level
        driver.findElementByXPath("//div[@class='level-drop-down']/div/span[@class='level-name'][text()='INFO']").click();

        waitFor(() -> getVisibleRecords().matches(".+INFO.+\n.+WARN.+\n.+ERROR.+"));

        String filtersHash = WebTestUtils.queryParameters(driver.getCurrentUrl()).get("filters");
        assertNotNull(filtersHash);

        assert editorHeader.getText().contains("INFO");
        assert !editorHeader.getText().contains("*");

        editorHeader.click();

        inputs = driver.findElementsByCssSelector(".level-drop-down > div > input");

        assertEquals("true", inputs.get(0).getAttribute("checked")); // ERROR
        assertEquals("true", inputs.get(1).getAttribute("checked")); // WARN
        assertEquals("true", inputs.get(2).getAttribute("checked")); // INFO
        assertNull(inputs.get(3).getAttribute("checked")); // DEBUG
        assertNull(inputs.get(4).getAttribute("checked")); // WARN

        // Unselect ERROR level
        inputs.get(0).click();

        assert editorHeader.getText().contains("INFO");
        assert editorHeader.getText().contains("*");

        waitFor(() -> getVisibleRecords().matches(".+INFO.+\n.+WARN.+"));

        String filtersHash2 = WebTestUtils.queryParameters(driver.getCurrentUrl()).get("filters");
        assertNotNull(filtersHash2);
        assertNotEquals(filtersHash, filtersHash2);

        inputs = driver.findElementsByCssSelector(".level-drop-down > div > input");

        assertNull(inputs.get(0).getAttribute("checked")); // ERROR
        assertEquals("true", inputs.get(1).getAttribute("checked")); // WARN
        assertEquals("true", inputs.get(2).getAttribute("checked")); // INFO
        assertNull(inputs.get(3).getAttribute("checked")); // DEBUG
        assertNull(inputs.get(4).getAttribute("checked")); // WARN

        // Select all levels back
        driver.findElementByXPath("//div[@class='level-drop-down']/div/span[@class='level-name'][text()='TRACE']").click();

        assert editorHeader.getText().contains("TRACE");
        assert !editorHeader.getText().contains("*");

        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() == 5);
        assertEquals(5, driver.findElementsByCssSelector("#records > .record").size());

        assertNull(WebTestUtils.queryParameters(driver.getCurrentUrl()).get("filters"));
    }

    @Test
    public void exceptionOnlyFilter() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);

        openLog("exeception-in-the-end.log");

        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() > 5);

        assert !driver.getCurrentUrl().contains("filters=");

        WebElement exceptionOnlyButton = driver.findElementByXPath("//lv-exception-only/span");

        exceptionOnlyButton.click();

        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() == 1);
        assert driver.getCurrentUrl().contains("filters=");
        assert exceptionOnlyButton.getAttribute("class").contains("tool-button-pressed");

        exceptionOnlyButton.click();

        waitFor(() -> driver.findElementsByCssSelector("#records > .record").size() > 5);
        assert !driver.getCurrentUrl().contains("filters=");
        assert !exceptionOnlyButton.getAttribute("class").contains("tool-button-pressed");
    }
}
