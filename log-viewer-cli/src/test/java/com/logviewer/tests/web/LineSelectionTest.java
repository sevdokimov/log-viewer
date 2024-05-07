package com.logviewer.tests.web;

import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.utils.TestLogFormats;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class LineSelectionTest extends AbstractWebTestCase {

    private final static String logText;
    static {
        String path = getDataFilePath("1-100.log");
        try {
            logText = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void selectUnselect() {
        openLog("1-100.log");

        WebElement r100 = recordByText("100");

        assertThat(r100.getAttribute("class")).doesNotContain("selected-line");
        assertThat(getFragment()).isNull();

        r100.click();

        assertThat(r100.getAttribute("class")).contains("selected-line");
        checkFragment(null, logText.indexOf("\n100") + 1);

        WebElement r96 = recordByText("96");

        assertThat(r96.getAttribute("class")).doesNotContain("selected-line");

        r96.click();

        assertThat(r96.getAttribute("class")).contains("selected-line");
        checkFragment(null, logText.indexOf("\n96") + 1);
        assertThat(r100.getAttribute("class")).doesNotContain("selected-line");

        new Actions(driver).keyDown(Keys.CONTROL).click(r96).keyUp(Keys.CONTROL).perform();

        assertThat(r96.getAttribute("class")).doesNotContain("selected-line");
        assertThat(getFragment()).isNull();
    }

    @Test
    public void openLogInPosition() {
        openLog("1-100.log");
        setHeight(5);

        driver.get(driver.getCurrentUrl() + '#' + createFragment(null, logText.indexOf("\n80") + 1));
        driver.navigate().refresh();

        WebElement selectedRecord = driver.findElement(By.cssSelector(".record.selected-line"));
        assertThat(selectedRecord.getText()).isEqualTo("80");

        assertThat(getVisibleRecords()).contains("\n80\n").doesNotContain("\n2\n", "\n100");
    }

    @Test
    public void openLogInPosition2() {
        openLog("1-100.log");
        setHeight(5);

        int line80Start = logText.indexOf("\n80") + 1;
        driver.get(driver.getCurrentUrl() + '#' + createFragment(null, line80Start + 1)); // middle of the record
        driver.navigate().refresh();

        WebElement selectedRecord = driver.findElement(By.cssSelector(".record.selected-line"));

        assertThat(selectedRecord.getText()).isEqualTo("80");

        // Check that no duplication of selected records
        assertThat(getVisibleRecords()).contains("\n79\n80\n81").doesNotContain("\n2\n", "\n100");
    }

    @Test
    public void dontStoreLineSelectionInHistory() {
        openUrl("/");

        String homeUrl = driver.getCurrentUrl();

        openLog("1-100.log");

        recordByText("100").click();
        checkFragment(null, logText.indexOf("100"));

        recordByText("98").click();
        checkFragment(null, logText.indexOf("98"));
        assertThat(recordByText("98").getAttribute("class")).contains("selected-line");

        String urlLog98 = driver.getCurrentUrl();

        driver.navigate().back();

        assertThat(driver.getCurrentUrl()).isEqualTo(homeUrl);

        driver.navigate().forward();

        assertThat(driver.getCurrentUrl()).isEqualTo(urlLog98);

        assertThat(recordByText("98").getAttribute("class")).contains("selected-line");
    }

    @Test
    public void selectRecordInMergedLog() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        openLog("multifile/log-a.log", "multifile/log-b.log");

        WebElement rSelected = driver.findElement(By.xpath("//div[@id='records']/div[@class='record labeled']/div[@class='rec-text'][normalize-space(.)='150101 10:00:01 a 1']/.."));

        rSelected.click();

        assertThat(rSelected.getAttribute("class")).contains("selected-line");
        checkFragment("log-a.log", 0);
    }

    @Test
    public void openMergedLogInPosition() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        openLog("multifile/log-a.log", "multifile/log-b.log");

        String baseUrl = driver.getCurrentUrl();

        driver.get(baseUrl + '#' + createFragment("log-b.log", 0));
        driver.navigate().refresh();

        WebElement selectedRecord = driver.findElement(By.cssSelector(".record.selected-line .rec-text"));
        assertThat(selectedRecord.getText()).isEqualTo("150101 10:00:01 b 1");

        driver.get(baseUrl + '#' + createFragment("log-a.log", 0));
        driver.navigate().refresh();

        waitFor(() -> {
            WebElement r = driver.findElement(By.cssSelector(".record.selected-line .rec-text"));
            return r.getText().equals("150101 10:00:01 a 1");
        });
    }

    @Test
    public void openLogWithIncorrectPosition() {
        openLog("1-100.log");
        setHeight(5);

        driver.get(driver.getCurrentUrl() + '#' + createFragment(null, 9999999));
        driver.navigate().refresh();

        recordByText("2");

        assertThat(getFragment()).isNull();
    }

    @Test
    public void fragmentAfterFiltersChange() {
        openLog("1-100.log");

        WebElement r99 = recordByText("99");

        r99.click();

        assertThat(r99.getAttribute("class")).contains("selected-line");
        checkFragment(null, logText.indexOf("\n99") + 1);

        assertThat(driver.getCurrentUrl()).doesNotContain("filters=");

        driver.findElement(ADD_FILTER_BUTTON).click();
        driver.findElement(By.id("add-text-filter")).click();

        WebElement input = driver.findElement(By.cssSelector("lv-text-filter textarea[name=text]"));

        input.click();
        input.sendKeys("99");

        driver.findElement(By.cssSelector("lv-text-filter button[name=apply-button]")).click();

        waitFor(() -> getVisibleRecords().equals("99"));

        assertThat(driver.getCurrentUrl()).contains("filters=");
        checkFragment(null, logText.indexOf("\n99") + 1);
        assertThat(r99.isDisplayed()).isTrue();
        assertThat(r99.getAttribute("class")).contains("selected-line");
    }

    private String createFragment(@Nullable String logId, int offset) {
        String fragment = "p" + offset;
        if (logId != null)
            fragment = logId + "-" + fragment;

        return fragment;
    }

    private void checkFragment(@Nullable String logId, int offset) {
        assertThat(getFragment()).isEqualTo(createFragment(logId, offset));
    }

    private String getFragment() {
        String currentUrl = driver.getCurrentUrl();
        int idx = currentUrl.indexOf('#');
        if (idx < 0)
            return null;

        return currentUrl.substring(idx + 1);
    }
}
