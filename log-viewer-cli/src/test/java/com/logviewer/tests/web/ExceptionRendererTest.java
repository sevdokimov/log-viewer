package com.logviewer.tests.web;

import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;

public class ExceptionRendererTest extends AbstractWebTestCase {

    @Before
    public void initFormat() {
        RegexLogFormat format = new RegexLogFormat(StandardCharsets.UTF_8, "msg",
                "\\[?(\\d{4}-\\d\\d-\\d\\d_\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d)]? (.*)",
                new RegexLogFormat.RegexpField("date", 1, FieldTypes.DATE),
                new RegexLogFormat.RegexpField("msg", 2, "message")
        );

        ctx.getBean(TestFormatRecognizer.class).setFormat(format);
    }

    @Test
    public void oneLineLog() throws InterruptedException {
        openLog("rendering/one-line-exception.log");

        WebElement classElement = driver.findElement(By.className("exception-class"));
        assertEquals("org.apache.catalina.connector.ClientAbortException", classElement.getText());
        WebElement img = classElement.findElement(By.xpath("./preceding-sibling::*"));
        assertEquals("img", img.getTagName());

        WebElement line = getOnlyElement(driver.findElements(By.className("ex-stacktrace-line")));
        assertEquals("org.apache.catalina.connector", line.findElement(By.className("ex-stacktrace-package")).getText());
        assertEquals("OutputBuffer", line.findElement(By.className("ex-stacktrace-class")).getText());
        assertEquals("java.net.SocketException: Broken pipe (Write failed)", driver.findElement(By.className("exception-message")).getText());

        notExist(By.className("coll-wrapper"));
    }

    @Test // If exception is at end of the log exception ends with '\n'
    public void exceptionWithLineEnd() throws InterruptedException {
        openLog("rendering/execption-with-line-end.log");

        driver.findElement(By.className("exception-class"));
    }

    @Test
    public void strangeLines() throws InterruptedException, IOException {
        String logPath = openLog("rendering/strange-exception-line.log");

        int lines = Files.readAllLines(Paths.get(logPath)).size();

        assertEquals(lines - 2, driver.findElements(By.className("ex-stacktrace-line")).size());
    }

}
