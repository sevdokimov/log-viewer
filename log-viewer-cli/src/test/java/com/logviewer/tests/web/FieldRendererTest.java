package com.logviewer.tests.web;

import com.logviewer.formats.RegexLogFormat;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.mocks.TestUiConfigurer;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.nio.charset.StandardCharsets;

public class FieldRendererTest extends AbstractWebTestCase {

    public static final RegexLogFormat FORMAT = new RegexLogFormat(StandardCharsets.UTF_8, "msg", "(\\d+)",
            new RegexLogFormat.RegexpField("msg", 1, "custom-field-type"));

    @Test
    public void styleForFieldType() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT);

        ctx.getBean(TestUiConfigurer.class).setConfig(ConfigFactory.parseString("" +
                "field-types {" +
                "    custom-field-type = {" +
                "        style {" +
                "            color: \"#00a\"\n" +
                "            fontWeight: bold\n" +
                "            fontStyle: italic\n" +
                "            className: zzz\n" +
                "        }" +
                "    }" +
                "}" +
                ""));

        openLog("1-7.log");

        WebElement element = driver.findElement(By.xpath("//div[@class='record']//*[@class='zzz'][text()='7']"));
        Assert.assertEquals("rgba(0, 0, 170, 1)", element.getCssValue("color"));
        Assert.assertEquals("700", element.getCssValue("font-weight"));
        Assert.assertEquals("italic", element.getCssValue("font-style"));
    }

    @Test
    public void testFixedTextRenderer() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT);

        ctx.getBean(TestUiConfigurer.class).setConfig(ConfigFactory.parseString("" +
                "field-types {" +
                "    custom-field-type = {" +
                "        textType: custom-text-type" +
                "    }" +
                "}\n" +
                "text-highlighters {" +
                "    custom-text-highlighter = {\n" +
                "        text-type: [custom-text-type]\n" +
                "        class: FixedTextRenderer\n" +
                "        args {\"7\": {className: zzz}}" +
                "    }" +
                "}" +

                ""));

        openLog("1-7.log");

        driver.findElement(By.xpath("//div[@class='record']//*[@class='zzz'][text()='7']"));
        notExist(By.xpath("//div[@class='record']//*[@class='zzz'][text()='6']"));
    }

    @Test
    public void testTextFieldRenderer() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(FORMAT);

        ctx.getBean(TestUiConfigurer.class).setConfig(ConfigFactory.parseString("" +
                "field-types {" +
                "    custom-field-type = {" +
                "        textType= ffffff\n" +

                "        class = TextFieldRenderer\n" +
                "        args { " +
                "            style: {fontWeight: bold}\n" +
                "            textType: custom-text-type\n" +
                "        }" +
                "    }" +
                "}\n" +
                "text-highlighters {" +
                "    custom-text-highlighter = {\n" +
                "        text-type: [custom-text-type]\n" +
                "        class: FixedTextRenderer\n" +
                "        args {\"7\": {className: zzz}}" +
                "    }" +
                "}" +

                ""));

        openLog("1-7.log");

        WebElement element = driver.findElement(By.xpath("//div[@class='record']//*[@class='zzz'][text()='7']"));
        Assert.assertEquals("700", element.getCssValue("font-weight"));
    }
}
