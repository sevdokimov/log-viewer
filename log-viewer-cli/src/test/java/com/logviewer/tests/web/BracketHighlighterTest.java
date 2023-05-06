package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;

public class BracketHighlighterTest extends AbstractWebTestCase {

    @Test
    public void testBracketHighlighting() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);

        openLog("rendering/brackets-highlighting.log");

        driver.findElement(By.className("text-milliseconds")); // wait for rendering
        List<WebElement> elements = driver.findElements(By.cssSelector(".text-milliseconds, .lv-bracket"));

        Assert.assertEquals(Arrays.asList("86400000ms", "(", "58532ms", "[", "]", ")", "{", "[", "]", "[", "{",
                "58532ms", "}", "]", "}", "58532ms", "(", "39532ms", ")"),
                elements.stream().map(WebElement::getText).collect(Collectors.toList()));

        WebElement classElement = driver.findElement(By.className("exception-class"));
        assertEquals("org.apache.catalina.connector.ClientAbortException", classElement.getText());
        WebElement img = classElement.findElement(By.xpath("./preceding-sibling::*"));
        assertEquals("img", img.getTagName());

        WebElement line = getOnlyElement(driver.findElements(By.className("ex-stacktrace-line")));
        assertEquals("org.apache.catalina.connector", line.findElement(By.className("ex-stacktrace-package")).getText());
        assertEquals("OutputBuffer", line.findElement(By.className("ex-stacktrace-class")).getText());
        assertEquals("java.net.SocketException: Broken pipe (39532ms)", driver.findElement(By.className("exception-message")).getText());
    }
}
