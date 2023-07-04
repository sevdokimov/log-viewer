package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import static org.assertj.core.api.Assertions.assertThat;

public class LineSelectionTest extends AbstractWebTestCase {

    @Test
    public void selectUnselect() {
        openLog("1-100.log");

        WebElement r100 = recordByText("100");

        assertThat(r100.getAttribute("class")).doesNotContain("selected-line");

        r100.click();

        assertThat(r100.getAttribute("class")).contains("selected-line");

        WebElement r96 = recordByText("96");

        assertThat(r96.getAttribute("class")).doesNotContain("selected-line");

        r96.click();

        assertThat(r96.getAttribute("class")).contains("selected-line");
        assertThat(r100.getAttribute("class")).doesNotContain("selected-line");

        new Actions(driver).keyDown(Keys.CONTROL).click(r96).keyUp(Keys.CONTROL).perform();

        assertThat(r96.getAttribute("class")).doesNotContain("selected-line");
    }
}
