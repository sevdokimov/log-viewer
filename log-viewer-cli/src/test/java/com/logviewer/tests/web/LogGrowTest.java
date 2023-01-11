package com.logviewer.tests.web;

import com.logviewer.mocks.TestFilterPanelState;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.utils.TestLogFormats;
import com.logviewer.utils.FilterPanelState;
import com.logviewer.utils.Utils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogGrowTest extends AbstractWebTestCase {

    @Test
    public void logCrashedOnPageUp() throws IOException {
        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, IntStream.rangeClosed(1, 20).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        openLog(tmpLog);
        setHeight(5);
        driver.navigate().refresh();

        checkLastRecord("20");

        Files.write(tmpLog, IntStream.rangeClosed(21, 40).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        new Actions(driver).sendKeys(Keys.PAGE_UP).perform();

        checkLastRecord("40");
    }

    @Test
    public void logCrashedOnSearchText() throws InterruptedException, IOException {
        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, IntStream.rangeClosed(1, 20).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        openLog(tmpLog);
        setHeight(5);
        driver.navigate().refresh();

        checkLastRecord("20");

        new Actions(driver).sendKeys(Keys.HOME).perform();

        recordByText("1");

        Files.write(tmpLog, IntStream.rangeClosed(21, 40).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        Thread.sleep(700);

        notExist(By.xpath("//div[@id='records']/div[@class='record'][text()='40']"));

        WebElement filterInput = driver.findElement(FilterPanel.INPUT);
        filterInput.sendKeys("fzdfsdfsdf", Keys.F3);

        checkLastRecord("40");
    }

    @Test
    public void logGrow() throws IOException {
        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, Utils.EMPTY_BYTE_ARRAY);

        openLog(tmpLog);
        setHeight(5);
        driver.navigate().refresh();

        driver.findElement(By.className("empty-log-message"));

        Files.write(tmpLog, "1".getBytes());

        checkLastRecord("1");

        Files.write(tmpLog, IntStream.rangeClosed(1, 2).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        checkLastRecord("2");

        Files.write(tmpLog, IntStream.rangeClosed(1, 4).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        checkLastRecord("4");

        Files.write(tmpLog, IntStream.rangeClosed(1, 30).mapToObj(String::valueOf).collect(Collectors.joining("\n")).getBytes());

        recordByText("5");

        notExist(By.xpath("//div[@id='records']/div[@class='record'][last()][text()='30']"));

        Number recordCount = (Number) driver.executeScript("return arguments[0].childElementCount", driver.findElementById("records"));
        assert recordCount.longValue() > 5;
        assert recordCount.longValue() < 29;

        recordByText("1");
    }

    @Test
    public void filterAll() throws IOException {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.FORMAT_LEVEL_LOG4j);

        ctx.getBean(TestFilterPanelState.class).addFilterSet("default", new FilterPanelState().setExceptionsOnly(true));

        Path tmpLog = tmpDir.resolve("test.log");
        Files.write(tmpLog, "160101 10:00:03 DEBUG ddd\n".getBytes());

        openUrl("log", "path", tmpLog.toFile().getAbsolutePath(), "path", getDataFilePath("level-log4j.log"));

        driver.findElement(By.xpath("//span[@class='no-record-msg'][text()='All records filtered']"));

        Files.write(tmpLog, ("160101 10:00:03 DEBUG ddd\n" +
                "160101 10:00:04 DEBUG ddd\n" +
                "160101 10:00:05 DEBUG ddd\n").getBytes());

        driver.findElement(By.xpath("//span[@class='file-attr'][text()='78 bytes']"));
        driver.findElement(By.xpath("//span[@class='no-record-msg'][text()='All records filtered']"));
    }

}
