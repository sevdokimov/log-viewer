package com.logviewer.tests.web;

import com.logviewer.impl.LvFileNavigationManagerImpl;
import com.logviewer.mocks.TestFormatRecognizer;
import com.logviewer.tests.pages.ChooserPage;
import com.logviewer.tests.pages.LogPage;
import com.logviewer.tests.utils.TestLogFormats;
import org.junit.Test;
import org.openqa.selenium.interactions.Actions;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class AddLogTest extends AbstractWebTestCase implements LogPage {

    @Test
    public void addLogFromMenu() {
        String fileA = getDataFilePath("multifile/log-a.log");

        ctx.getBean(LvFileNavigationManagerImpl.class).setDefaultDirectory(Paths.get(fileA).getParent().toString());

        try {
            ctx.getBean(TestFormatRecognizer.class).setFormat(TestLogFormats.MULTIFILE);

            openUrl("log");

            driver.findElement(MESSAGE_NO_LOG_PATH);

            driver.findElement(MENU).click();

            driver.findElement(Menu.ITEM_ADD_LOG).click();

            String currentUrl = driver.getCurrentUrl();
            ChooserPage.findDirectoryNameRef("multifile").click();
            assertEquals(driver.getCurrentUrl(), currentUrl);

            new Actions(driver).doubleClick(ChooserPage.findFile("log-a.log")).perform();

            waitFor(() -> getRecord().size() == 4);
            notExist(ChooserPage.CHOOSER);

            driver.findElement(MENU).click();
            driver.findElement(Menu.ITEM_ADD_LOG).click();

            new Actions(driver).doubleClick(ChooserPage.findFile("log-a.log")).perform(); // Selection existing file
            closeInfoAlert();
            
            notExist(ChooserPage.CHOOSER);
            waitFor(() -> getRecord().size() == 4);

            driver.findElement(MENU).click();
            driver.findElement(Menu.ITEM_ADD_LOG).click();

            new Actions(driver).doubleClick(ChooserPage.findFile("log-b.log")).perform();

            waitFor(() -> getRecord().size() == 9);
            assert driver.getCurrentUrl().contains("log-a.log");
            assert driver.getCurrentUrl().contains("log-b.log");
        } finally {
            ctx.getBean(LvFileNavigationManagerImpl.class).setDefaultDirectory("");
        }
    }
}
