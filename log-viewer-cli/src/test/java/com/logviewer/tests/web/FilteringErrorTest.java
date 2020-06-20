package com.logviewer.tests.web;

import com.logviewer.data2.Filter;
import com.logviewer.domain.FilterPanelState;
import com.logviewer.filters.GroovyPredicate;
import com.logviewer.mocks.TestFilterPanelState;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FilteringErrorTest extends AbstractWebTestCase {

    @Test
    public void filteringError() throws InterruptedException, IOException {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState()
                .setNamedFilters(new Filter(null, false, new GroovyPredicate("class AAA _ {")),
                        new Filter(null, false, new GroovyPredicate("_ in [\"4\", \"7\"] ? zzz.ppppp : false"))));

        openLog("1-7.log");

        checkLastRecord("7");
        notExist(By.className("filtering-error"));

        filterState(0, true);

        assertEquals(7, driver.findElementsByClassName("filtering-error").size());
        filterState(0, false);
        filterState(1, true);

        waitFor(() -> driver.findElementsByClassName("filtering-error").size() == 2);

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
