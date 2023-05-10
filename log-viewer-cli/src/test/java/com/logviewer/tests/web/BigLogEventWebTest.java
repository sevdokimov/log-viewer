package com.logviewer.tests.web;

import com.logviewer.data2.ParserConfig;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.utils.TestLogFormats;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

public class BigLogEventWebTest extends AbstractWebTestCase {

    public static final By HAS_MORE = By.cssSelector(".record .has-more .has-more-load-more");

    @Test
    public void testBracketHighlighting() {
        ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.SIMPLE_FORMAT);

        openLog("big-log-event.log");

        WebElement hasMoreLink = driver.findElement(By.cssSelector(".record .has-more .has-more-load-more"));

        notExist(By.cssSelector(".record .lv-bracket"));

        By longEventText = By.cssSelector(".record:nth-child(1) .rec-text");

        String text = driver.findElement(longEventText).getText();
        assert text.length() < ParserConfig.MAX_LINE_LENGTH + 1000;

        hasMoreLink.click();

        waitFor(() -> driver.findElement(longEventText).getText().length() > text.length());

        new Actions(driver).sendKeys(Keys.END).perform();

        WebElement hasMoreLink2 = driver.findElement(HAS_MORE);

        hasMoreLink2.click();

        List<WebElement> brackets = driver.findElements(By.cssSelector(".record .lv-bracket"));
        Assertions.assertThat(brackets).hasSize(2);

        notExist(HAS_MORE);
    }


}
