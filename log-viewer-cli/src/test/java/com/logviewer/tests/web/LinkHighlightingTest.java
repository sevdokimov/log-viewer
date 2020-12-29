package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class LinkHighlightingTest extends AbstractWebTestCase {

    @Test
    public void testLinkHighlighting() {
        openLog("log-with-link.log");

        driver.findElementByClassName("link-in-log");

        assertEquals(Arrays.asList("http://localhost:8080/?aaa=bbb&ccc=12", "https://google.com",
                "https://my-host.com/foo#anchor-w3234"),
                driver.findElementsByClassName("link-in-log").stream().map(WebElement::getText).collect(Collectors.toList())
                );
    }

}
