package com.logviewer.tests.web;

import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFormat;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.utils.FilterPanelState;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

import static com.logviewer.tests.utils.TestLogFormats.FORMAT_LEVEL_LOG4j;
import static com.logviewer.tests.utils.TestLogFormats.FORMAT_LEVEL_LOGBACK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LogLevelTest extends AbstractWebTestCase {

    private static final LogFormat FORMAT_JAVA = new RegexLogFormat(
            "(?<date>\\d{6} \\d\\d:\\d\\d:\\d\\d) (?<level>\\w+) (?<msg>.*)",
            "yyMMdd HH:mm:ss", "date",
            RegexLogFormat.field("date", FieldTypes.DATE),
            RegexLogFormat.field("level", FieldTypes.LEVEL),
            RegexLogFormat.field("msg", "message")
            );

    @Test
    public void log4jFilterTest() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT_LEVEL_LOG4j);

        openLog("level-log4j.log");

        driver.findElement(By.cssSelector("lv-level-list > div > span")).click();

        List<WebElement> levelName = driver.findElements(By.cssSelector(".level-drop-down .level-name"));
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "FATAL,ERROR,WARN,INFO,DEBUG,TRACE"
        ));
    }

    @Test
    public void logbackFilterTest() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT_LEVEL_LOGBACK);

        openLog("level-logback.log");

        driver.findElement(By.cssSelector("lv-level-list > div > span")).click();

        List<WebElement> levelName = driver.findElements(By.cssSelector(".level-drop-down .level-name"));
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "ERROR,WARN,INFO,DEBUG,TRACE"
        ));
    }

    @Test
    public void javaFilterTest() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT_JAVA);

        openLog("level-logback.log");

        driver.findElement(By.cssSelector("lv-level-list > div > span")).click();

        List<WebElement> levelName = driver.findElements(By.cssSelector(".level-drop-down .level-name"));
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "ERROR,WARN,INFO,DEBUG,TRACE"
        ));
    }

    @Test
    public void formatCombination() {
        TestFormatRecognizer recognizer = ctx.getBean(TestFormatRecognizer.class);
        recognizer.setFormat(getDataFilePath("level-log4j.log"), FORMAT_LEVEL_LOG4j);
        recognizer.setFormat(getDataFilePath("level-java.log"), FORMAT_JAVA);
        recognizer.setFormat(getDataFilePath("level-logback.log"), FORMAT_LEVEL_LOGBACK);

        // ERROR
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("ERROR"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+ERROR.+\n.+ERROR.+\n.+SEVERE.+\n.+FATAL.+"));

        // WARN
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("WARN"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+\\bWARN\\b.+\n.+\\bWARNING\\b.+\n.+\\bWARN\\b.+"));

        // INFO
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("INFO"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+CONFIG.+\n.+INFO.+\n.+INFO.+\n.+INFO.+"));

        // DEBUG
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("DEBUG"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+DEBUG.+\n.+FINER.+\n.+FINE.+\n.+DEBUG.+"));

        // TRACE
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("TRACE"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+TRACE.+\n.+FINEST.+\n.+TRACE.+"));

        // Test filter items
        driver.findElement(By.cssSelector("lv-level-list > div > span")).click();

        List<WebElement> levelName = driver.findElements(By.cssSelector(".level-drop-down .level-name"));
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "ERROR,WARN,INFO,DEBUG,TRACE"
        ));

    }


}
