package com.logviewer.tests.web;

import com.logviewer.formats.RegexLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.mocks.TestUiConfigurer;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class FieldReorderTest extends AbstractWebTestCase {

    public static final RegexLogFormat FORMAT = new RegexLogFormat(StandardCharsets.UTF_8,
            "(\\[[^\\]]+\\])\\[ *([^\\]]+)\\] (\\w+) (\\d+) (\\d+)", true,
            new RegexLogFormat.RegexField("num2", 4, "number"),
            new RegexLogFormat.RegexField("num1", 5, "number"),
            new RegexLogFormat.RegexField("f3", 3),
            new RegexLogFormat.RegexField("f2", 2),
            new RegexLogFormat.RegexField("date", 1, "my-date")
            );

    @Test
    public void styleForFieldType() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT);

        ctx.getBean(TestUiConfigurer.class).setConfig(ConfigFactory.parseString("" +
                "field-types {" +
                "    number = {" +
                "        style { className: nnn }" +
                "    }\n" +
                "    my-date = {" +
                "        style { className: ddd }" +
                "    }\n" +
                "}" +
                ""));

        openLog("search.log");

        WebElement element = lastRecord();

        assertEquals("[2012.01.01 00:44][     :::::] sss 44 44", element.getText());

        assertEquals(2, element.findElements(By.className("nnn")).size());
        WebElement date = element.findElement(By.className("ddd"));
        assertEquals("[2012.01.01 00:44]", date.getText());
    }
}
