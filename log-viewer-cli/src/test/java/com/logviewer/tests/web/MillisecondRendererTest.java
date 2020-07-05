package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.data2.LogService;
import com.logviewer.logLibs.logback.LogbackLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MillisecondRendererTest extends AbstractWebTestCase {

    @Test
    public void testMilliseconds() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);

        openLog("rendering/milliseconds.log");

        driver.findElementByClassName("text-milliseconds"); // wait for rendering
        List<WebElement> elements = driver.findElementsByClassName("text-milliseconds");

        checkAllMillis(elements);

        elements = driver.findElementsByCssSelector(".exception-message .text-milliseconds");
        Assert.assertEquals(Arrays.asList("39532ms", "18522ms"),
                elements.stream().map(WebElement::getText).collect(Collectors.toList()));
    }

    @Test
    public void testMillisecondsWrongFormat() {
        // The format of file is different
        ctx.getBean(TestFormatRecognizer.class).setFormat(new LogbackLogFormat("%date{yyyy-MM-dd HH:mm:ss.SSS} %msg%n"));

        openLog("rendering/milliseconds.log");

        driver.findElementByClassName("text-milliseconds"); // wait for rendering
        List<WebElement> elements = driver.findElementsByClassName("text-milliseconds");

        checkAllMillis(elements);
    }

    @Test
    public void testMillisecondsSimpleFormat() {
        // The format of file is different
        ctx.getBean(TestFormatRecognizer.class).setFormat(LogService.DEFAULT_FORMAT);

        openLog("rendering/milliseconds.log");

        driver.findElementByClassName("text-milliseconds"); // wait for rendering
        List<WebElement> elements = driver.findElementsByClassName("text-milliseconds");

        checkAllMillis(elements);
    }

    private void checkAllMillis(List<WebElement> elements) {
        Assert.assertEquals(Arrays.asList("3666ms", "3777 ms",
                "11444ms", "63123ms", "64000ms",  "3600000ms", "3702030ms",  "8640000ms",  "17280000ms",  "17280000ms",
                "20880000ms", "86400000ms", "172800000ms", "181440000ms", "58532ms", "39532ms", "18522ms"),
                elements.stream().map(WebElement::getText).collect(Collectors.toList()));

        Assert.assertEquals(Arrays.asList("3.666s", "3.777s", "11s", "1min 3s", "1min 4s", "01:00:00", "01:01:42",
                "02:24:00", "04:48:00", "04:48:00", "05:48:00", "1day", "2days", "2days 02:24:00", "59s", "40s", "19s"),
                elements.stream().map(e -> e.getAttribute("title")).collect(Collectors.toList()));
    }

    @Test
    public void dateInMilliseconds() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);

        openLog("rendering/date-in-milliseconds.log");

        List<WebElement> elements = driver.findElementsByClassName("text-date-in-milliseconds");

        Assert.assertEquals(Arrays.asList("1552698301463", "1552699301463"),
                elements.stream().map(WebElement::getText).collect(Collectors.toList()));

        assert elements.get(0).getAttribute("title").matches("2019-03-1\\d, \\d\\d:05:01 \\w+\\+\\d+");
    }

}
