package com.logviewer.tests.web;

import com.google.common.collect.Iterables;
import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;

public class ExceptionRendererTest extends AbstractWebTestCase {

    @Before
    public void initFormat() {
        RegexLogFormat format = new RegexLogFormat(
                "\\[?(\\d{4}-\\d\\d-\\d\\d_\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d)]? (.*)",
                new RegexLogFormat.RegexField("date", 1, FieldTypes.DATE),
                new RegexLogFormat.RegexField("msg", 2, "message")
        );

        ctx.getBean(TestFormatRecognizer.class).setFormat(format);
    }

    @Test
    public void oneLineLog() {
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
    public void exceptionWithLineEnd() {
        openLog("rendering/execption-with-line-end.log");

        driver.findElement(By.className("exception-class"));
    }

    @Test
    public void exceptionInvalidMessage() {
        openLog("rendering/execption-invalid-multiline-message.log");

        checkRecordCount(1);
        notExist(By.className("exception-class"));
    }

    @Test
    public void strangeLines() throws IOException {
        String logPath = openLog("rendering/strange-exception-line.log");

        int lines = Files.readAllLines(Paths.get(logPath)).size();

        assertEquals(lines - 2, driver.findElements(By.className("ex-stacktrace-line")).size());
    }

    @Test
    public void exceptions() {
        openLog("rendering/exceptions.log");

        List<WebElement> exceptions = driver.findElementsByClassName("ex-wrapper");
        assertEquals(5, exceptions.size());

        WebElement npe1 = exceptions.get(0);
        assertEquals("java.lang.NullPointerException", npe1.findElement(By.className("exception-class")).getText());
        assertEquals("the exception 1", npe1.findElement(By.className("exception-message")).getText());
        assertEquals(2, npe1.findElements(By.className("ex-stacktrace-line")).size());

        WebElement iae2 = exceptions.get(1);
        assertEquals("java.lang.IllegalArgumentException", iae2.findElement(By.className("exception-class")).getText());
        assertEquals("multiline\nmessage in excepion\n111", iae2.findElement(By.className("exception-message")).getText());
        assertEquals(2, iae2.findElements(By.className("ex-stacktrace-line")).size());

        WebElement re3 = exceptions.get(2);
        assertEquals("java.lang.RuntimeException", re3.findElement(By.className("exception-class")).getText());
        super.noImplicitWait(() -> assertEquals(0, re3.findElements(By.className("exception-message")).size()));
        assertEquals(2, re3.findElements(By.cssSelector(".ex-wrapper > .ex-stacktrace-line")).size());
        assertEquals(6, re3.findElements(By.cssSelector(".ex-stacktrace-line")).size());

        WebElement re4 = exceptions.get(3);
        assertEquals("java.lang.NullPointerException", re4.findElement(By.className("exception-class")).getText());
        assertEquals(2, re4.findElements(By.className("ex-stacktrace-line")).size());

        WebElement re5 = exceptions.get(4);
        assertEquals("java.io.IOException", re5.findElement(By.className("exception-class")).getText());
        assertEquals(2, re5.findElements(By.className("ex-stacktrace-line")).size());
    }

    @Test
    public void exceptionWithJarname() {
        openLog("rendering/exceptions-with-jarname.log");

        WebElement npe1 = Iterables.getOnlyElement(driver.findElementsByClassName("ex-wrapper"));
        assertEquals("java.lang.NullPointerException", npe1.findElement(By.className("exception-class")).getText());
        assertEquals(8, npe1.findElements(By.className("ex-stacktrace-line")).size());
    }

    @Test
    public void loadBottomAfterCollapse() {
        openLog("rendering/load-bottom-after-collapse.log");
        setHeight(8);
        driver.navigate().refresh();

        checkLastRecord("2017-09-25_12:38:59.686 [http-bio-8088-exec-5] zzz");

        new Actions(driver).sendKeys(Keys.HOME).perform();

        driver.findElement(By.cssSelector(".coll-expander")).click();

        for (int i = 0; i < 8; i++) {
            new Actions(driver).sendKeys(Keys.DOWN).perform();
        }

        for (int i = 0; i < 8; i++) {
            new Actions(driver).sendKeys(Keys.UP).perform();
        }

        driver.findElement(By.cssSelector(".coll-collapser")).click();

        checkLastRecord("2017-09-25_12:38:59.686 [http-bio-8088-exec-5] zzz");
    }

}
