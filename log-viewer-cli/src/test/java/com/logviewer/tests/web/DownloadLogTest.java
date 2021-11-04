package com.logviewer.tests.web;

import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.pages.LogPage;
import com.logviewer.tests.utils.TestLogFormats;
import com.logviewer.tests.utils.WebTestUtils;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.web.session.tasks.SearchPattern;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

@SuppressWarnings("CssInvalidHtmlTagReference")
public class DownloadLogTest extends AbstractWebTestCase {

    private static final By SUBMIT_BUTTON = By.cssSelector("lv-download-dialog button[type=submit]");;
    private static final By FILTER_NOTIFICATION = By.cssSelector("lv-download-dialog .filters-notification");
    public static final By ZIP_CHECKBOX = By.cssSelector("lv-download-dialog .zip-selection input");

    @Test
    public void nameValidation() {
        openLog("1-7.log");

        openDownloadDialog();

        WebElement nameField = driver.findElement(By.cssSelector("lv-download-dialog #fileName:focus"));
        new Actions(driver).sendKeys(Keys.DELETE,Keys.DELETE,Keys.DELETE,Keys.DELETE,Keys.DELETE).perform(); // "1-7.log" must be selected, so we can start type.
        assertEquals("", nameField.getAttribute("value"));

        assertDisabled(SUBMIT_BUTTON);
    }

    private void openDownloadDialog() {
        driver.findElement(LogPage.MENU).click();
        driver.findElement(Menu.DOWNLOAD_LOG).click();
    }

    @Test
    public void multilogCorrect() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        openLog("multifile/log-a.log", "multifile/log-b.log");

        openDownloadDialog();

        WebElement fileName = driver.findElement(By.cssSelector("lv-download-dialog #fileName"));
        assertEquals("log-a+log-b.log", fileName.getAttribute("value"));

        assertDisabled(ZIP_CHECKBOX);
        assertEquals("true", driver.findElement(ZIP_CHECKBOX).getAttribute("checked"));
    }

    @Test
    public void multilogInvalidLogs() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        String logPath = getDataFilePath("multifile/log-a.log");

        openUrl("log", Collections.singletonMap("path", Arrays.asList(logPath, "/opt/zzz.log", "/opt/xxx.log")));
//        openUrl("log", Collections.singletonMap("path", Arrays.asList(logPath, "/opt/zzz.log", logPath + "xxx.log")));

        openDownloadDialog();

        WebElement fileName = driver.findElement(By.cssSelector("lv-download-dialog #fileName:focus"));// Check file name has focus after reload
        assertEquals("log-a.log", fileName.getAttribute("value"));

        List<WebElement> inputs = driver.findElements(By.cssSelector("lv-download-dialog .log-table td:nth-child(1) input"));

        assertEquals(Arrays.asList("true", null, null), inputs.stream().map(e -> e.getAttribute("checked")).collect(Collectors.toList()));
        assertEquals(Arrays.asList(null, "true", "true"), inputs.stream().map(e -> e.getAttribute("disabled")).collect(Collectors.toList()));

        assertEnabled(ZIP_CHECKBOX);
        assertNull(driver.findElement(ZIP_CHECKBOX).getAttribute("checked"));
    }

    @Test
    public void singleLog() throws InterruptedException, IOException {
        openLog("1-7.log");

        openDownloadDialog();

        driver.findElement(By.cssSelector("lv-download-dialog #fileName:focus")); // Check file name has focus

        driver.findElement(By.cssSelector("#download-dialog .justify-content-end button")).click(); // close dialog
        notExist(By.cssSelector("lv-download-dialog"));

        // Reopen
        openDownloadDialog();
        WebElement fileName = driver.findElement(By.cssSelector("lv-download-dialog #fileName:focus"));// Check file name has focus after reload
        assertEquals("1-7.log", fileName.getAttribute("value"));
        new Actions(driver).sendKeys("zzz").perform(); // "1-7.log" must be selected, so we can start type.
        assertEquals("zzz.log", fileName.getAttribute("value"));

        assertEnabled(ZIP_CHECKBOX);
        assertNull(driver.findElement(ZIP_CHECKBOX).getAttribute("checked"));

        WebElement checkbox = driver.findElement(By.cssSelector("lv-download-dialog .log-table td:nth-child(1) input"));
        String checked = checkbox.getAttribute("checked");
        assertEquals("true", checked);

        notExist(FILTER_NOTIFICATION);

        // check that Submit button is disabled if no logs are selected
        checkbox.click();
        assertDisabled(SUBMIT_BUTTON);
        checkbox.click();
        assertEnabled(SUBMIT_BUTTON);

        Path downloaded = doDownload();
        assertEquals("zzz.log", downloaded.getFileName().toString());
        assertEquals(Files.readAllLines(Paths.get(getDataFilePath("1-7.log"))), Files.readAllLines(downloaded));
    }

    @Test
    public void downloadOneAsZip() throws InterruptedException, IOException {
        openLog("1-7.log");

        openDownloadDialog();

        WebElement zipCheckbox = driver.findElement(ZIP_CHECKBOX);
        assertNull(zipCheckbox.getAttribute("checked"));
        zipCheckbox.click();

        Path downloaded = doDownload();
        assertEquals("1-7.log.zip", downloaded.getFileName().toString());

        try (ZipFile zip = new ZipFile(downloaded.toFile())) {
            assertEquals(1, Collections.list(zip.entries()).size());

            ZipEntry entry = zip.getEntry("1-7.log");
            byte[] bytes = StreamUtils.copyToByteArray(zip.getInputStream(entry));
            compareWithFile("1-7.log", bytes);
        }
    }

    @Test
    public void downloadMultilog() throws IOException, InterruptedException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        openLog("multifile/log-a.log", "multifile/log-b.log");

        openDownloadDialog();

        Path downloaded = doDownload();
        assertEquals("log-a+log-b.log.zip", downloaded.getFileName().toString());

        try (ZipFile zip = new ZipFile(downloaded.toFile())) {
            assertEquals(2, Collections.list(zip.entries()).size());

            ZipEntry a = zip.getEntry("log-a.log");
            byte[] bytesA = StreamUtils.copyToByteArray(zip.getInputStream(a));
            compareWithFile("multifile/log-a.log", bytesA);

            ZipEntry b = zip.getEntry("log-b.log");
            byte[] bytesB = StreamUtils.copyToByteArray(zip.getInputStream(b));
            compareWithFile("multifile/log-b.log", bytesB);
        }
    }

    @Test
    public void downloadNonCheckedLog() throws IOException, InterruptedException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

        openLog("multifile/log-a.log", "multifile/log-b.log");

        openDownloadDialog();

        WebElement checkbox = driver.findElement(By.cssSelector("lv-download-dialog .log-table tr:nth-child(1) td:nth-child(1) input"));
        assertEquals("true", checkbox.getAttribute("checked"));
        checkbox.click();
        
        Path downloaded = doDownload();

        try (ZipFile zip = new ZipFile(downloaded.toFile())) {
            List<String> fileNames = Collections.list(zip.entries()).stream().map(ZipEntry::getName).collect(Collectors.toList());
            assertEquals(Collections.singletonList("log-b.log"), fileNames);
        }
    }

    @Test
    public void downloadWithFilters() throws IOException, InterruptedException {
        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().textFilter(
                new FilterPanelState.TextFilter("1", "", new SearchPattern("5|6", false, true), false)
        ));

        openLog("1-7.log");

        openDownloadDialog();

        checkRecordCount(2);

        Path downloaded = doDownload();

        assertEquals(Arrays.asList("5", "6"), Files.readAllLines(downloaded));
    }

    private static void compareWithFile(String fileName, byte[] data) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(getDataFilePath(fileName)));
        assertArrayEquals(clearFinalLineSeparator(fileContent), clearFinalLineSeparator(data));
    }

    private static byte[] clearFinalLineSeparator(byte[] data) {
        if (data.length > 0 && data[data.length - 1] == 10)
            return Arrays.copyOfRange(data, 0, data.length - 1);

        return data;
    }

    private Path doDownload() throws IOException, InterruptedException {
        Path downloadDir = WebTestUtils.getDownloadDirectory();

        Files.list(downloadDir).forEach(f -> f.toFile().delete());
        assertEquals(0, Files.list(downloadDir).count());

        driver.findElement(SUBMIT_BUTTON).click();
        Thread.sleep(200);

        Path[] paths = Files.list(downloadDir).toArray(Path[]::new);
        assertEquals(1, paths.length);

        return paths[0];
    }
}
