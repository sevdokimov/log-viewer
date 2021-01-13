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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldRendererTest extends AbstractWebTestCase {

    public static final RegexLogFormat FORMAT = new RegexLogFormat(StandardCharsets.UTF_8, "(\\d+)", false,
            new RegexLogFormat.RegexField("msg", 1, "custom-field-type"));

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
        assertThat(element.getCssValue("color"), anyOf(is("rgba(0, 0, 170, 1)"), is("rgb(0, 0, 170)")));
        assertThat(element.getCssValue("font-weight"), is("700"));
        assertThat(element.getCssValue("font-style"), is("italic"));
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
