package com.logviewer.tests.web;

import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFormat;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.utils.FilterPanelState;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LogLevelTest extends AbstractWebTestCase {

    private static final LogFormat FORMAT_LOG4j = new Log4jLogFormat("%d{yyMMdd HH:mm:ss} %p %m%n");
    private static final LogFormat FORMAT_LOGBACK = new LogbackLogFormat("%d{yyMMdd HH:mm:ss} %p %m%n");

    private static final LogFormat FORMAT_JAVA = new RegexLogFormat(StandardCharsets.UTF_8,
            "(?<date>\\d{6} \\d\\d:\\d\\d:\\d\\d) (?<level>\\w+) (?<msg>.*)", false,
            "yyMMdd HH:mm:ss", "date",
            RegexLogFormat.field("date", FieldTypes.DATE),
            RegexLogFormat.field("level", FieldTypes.LEVEL),
            RegexLogFormat.field("msg", "message")
            );

    @Test
    public void log4jFilterTest() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT_LOG4j);

        openLog("level-log4j.log");

        driver.findElementByCssSelector("lv-level-list > div > span").click();

        List<WebElement> levelName = driver.findElementsByCssSelector(".level-drop-down .level-name");
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "FATAL,ERROR,WARN,INFO,DEBUG,TRACE"
        ));
    }

    @Test
    public void logbackFilterTest() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT_LOGBACK);

        openLog("level-logback.log");

        driver.findElementByCssSelector("lv-level-list > div > span").click();

        List<WebElement> levelName = driver.findElementsByCssSelector(".level-drop-down .level-name");
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "ERROR,WARN,INFO,DEBUG,TRACE"
        ));
    }

    @Test
    public void javaFilterTest() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT_JAVA);

        openLog("level-logback.log");

        driver.findElementByCssSelector("lv-level-list > div > span").click();

        List<WebElement> levelName = driver.findElementsByCssSelector(".level-drop-down .level-name");
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "ERROR,WARN,INFO,DEBUG,TRACE"
        ));
    }

    @Test
    public void formatCombination() {
        TestFormatRecognizer recognizer = ctx.getBean(TestFormatRecognizer.class);
        recognizer.setFormat(getDataFilePath("level-log4j.log"), FORMAT_LOG4j);
        recognizer.setFormat(getDataFilePath("level-java.log"), FORMAT_JAVA);
        recognizer.setFormat(getDataFilePath("level-logback.log"), FORMAT_LOGBACK);

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
        waitFor(() -> getVisibleRecords().matches(".+INFO.+\n.+INFO.+\n.+INFO.+"));

        // DEBUG
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("DEBUG"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+DEBUG.+\n.+CONFIG.+\n.+DEBUG.+"));

        // TRACE
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setLevel("TRACE"));
        openLog("level-logback.log", "level-log4j.log", "level-java.log");
        waitFor(() -> getVisibleRecords().matches(".+TRACE.+\n.+FINEST.+\n.+FINER.+\n.+FINE.+\n.+TRACE.+"));

        // Test filter items
        driver.findElementByCssSelector("lv-level-list > div > span").click();

        List<WebElement> levelName = driver.findElementsByCssSelector(".level-drop-down .level-name");
        assertThat(levelName.stream().map(WebElement::getText).collect(Collectors.joining(",")), is(
                "ERROR,WARN,INFO,DEBUG,TRACE"
        ));

    }


}
