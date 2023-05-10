package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.pages.LogPage;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ViewLogIntegrationTest extends AbstractWebTestCase implements LogPage {

    @Test
    public void highlightOnType() {
        openLog("search.log");
        setHeight(5);
        driver.navigate().refresh();

        // Log is opened in the tail
        WebElement recordsParent = driver.findElement(By.id("records"));

        List<WebElement> records = recordsParent.findElements(By.cssSelector(".record"));
        assertEquals(12, records.size());

        assert records.get(records.size() - 1).getAttribute("textContent").startsWith("[2012.01.01 00:44]");

        // Scroll to head
        new Actions(driver).sendKeys(Keys.HOME).perform();

        WebDriverWait driverWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        driverWait.until(o -> {
            WebElement r = recordsParent.findElement(By.cssSelector(".record:first-child"));
            return r.getAttribute("textContent").startsWith("[2012.01.01 00:00]");
        });

        records = recordsParent.findElements(By.cssSelector(".record"));
        assert records.get(0).getAttribute("textContent").startsWith("[2012.01.01 00:00]");

        notExist(By.id("log-end"));
    }

    @Test
    public void testLogViewFavoritesIcon() throws InterruptedException {
        String filePath = openLog("empty.log");

        driver.findElement(MENU).click();

        WebElement favIcon = driver.findElement(By.className("favorite-icon"));
        assert !favIcon.getAttribute("className").contains("in-favorites");

        WebElement favLink = favIcon.findElement(By.xpath(".."));

        favLink.click();

        Thread.sleep(200);

        assert favIcon.getAttribute("className").contains("in-favorites");
        assert favoriteLogService.getFavorites().contains(filePath);

        driver.findElement(MENU).click();
        favLink.click();
        Thread.sleep(200);

        assert !favIcon.getAttribute("className").contains("in-favorites");
        assert !favoriteLogService.getFavorites().contains(filePath);
    }

    @Test
    public void initPosition() throws InterruptedException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestUtils.MULTIFILE_LOG_FORMAT);

        openLog("exeception-in-the-end.log");

        driver.manage().window().setSize(new Dimension(WINDOW_WIDTH, 300));

        openLog("exeception-in-the-end.log");

        WebElement logEnd = driver.findElement(By.className("log-end"));

        Thread.sleep(100);

        Point startLocation = logEnd.getLocation();

        assertEquals(startLocation, logEnd.getLocation());

        new Actions(driver).sendKeys(Keys.DOWN).perform();

        assertEquals(startLocation, logEnd.getLocation());

        new Actions(driver).sendKeys(Keys.UP).perform();

        assert startLocation.y < logEnd.getLocation().y;

        new Actions(driver).sendKeys(Keys.DOWN).perform();

        assertEquals(startLocation, logEnd.getLocation());
    }
}
