package com.logviewer.tests.web;

import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.utils.FilterPanelState.JsFilter;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

public class FilteringErrorTest extends AbstractWebTestCase {

    @Test
    public void filteringError() {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState()
                .jsFilter(new JsFilter("id", "", "function (text) { return ppppp == 'aaa' }")));

        openLog("1-7.log");

        checkLastRecord("7");

        assertEquals(7, driver.findElementsByClassName("filtering-error").size());

        WebElement r7 = recordByText("7");
        WebElement errorIcon = r7.findElement(By.className("filtering-error"));
        errorIcon.click();

        WebElement model = driver.findElementById("filter-error-modal");
        assert model.isDisplayed();
        assert model.getText().contains("ppppp");

        driver.findElementById("close-filtering-error-dialog").click();

        notExist(By.id("filter-error-modal"));
    }

}
