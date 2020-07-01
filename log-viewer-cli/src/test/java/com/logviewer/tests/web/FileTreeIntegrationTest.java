package com.logviewer.tests.web;

import com.google.common.collect.Sets;
import com.logviewer.impl.LvFileAccessManagerImpl;
import com.logviewer.impl.LvFileNavigationManagerImpl;
import com.logviewer.web.LogNavigatorController;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;

public class FileTreeIntegrationTest extends AbstractWebTestCase {

    protected static LvFileAccessManagerImpl accessManager() {
        return ctx.getBean(LvFileAccessManagerImpl.class);
    }

    @Test
    public void singleVisibleDirectoryIsExpand() {
        allowDatadirOnly();

        openUrl("/");

        WebElement fileTreeDiv = driver.findElement(By.id("file-tree"));

        noImplicitWait(() -> {
            assertEquals(1, fileTreeDiv.findElements(By.xpath("./sl-fs-tree-item")).size());
            try {
                assertEquals(Files.list(dataDir).filter(Files::isRegularFile).count(), fileTreeDiv.findElements(By.className("fs-item-file")).size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Select element
        WebElement rootItem = getOnlyElement(driver.findElements(By.cssSelector("#file-tree > sl-fs-tree-item > div.fs-item")));
        assert !rootItem.getAttribute("className").contains("selected");
    }

    @Test
    public void disableFileTree() {
        accessManager().setAllowedPaths(Collections.emptyList());

        System.setProperty(LogNavigatorController.CFG_SHOW_FILE_TREE, "false");

        try {
            openUrl("/");

            driver.findElement(By.cssSelector("input[name=pathToOpen]"));

            notExist(By.id("file-tree"));
            notExist(By.tagName("sl-fs-tree-item"));
        } finally {
            System.clearProperty(LogNavigatorController.CFG_SHOW_FILE_TREE);
        }
    }

    @Test
    public void testFileTreeFavorites() throws InterruptedException {
        allowDatadirOnly();

        String filePath = getDataFilePath("empty.log");

        favoriteLogService.getFavorites().forEach(f -> favoriteLogService.removeFavorite(f));

        favoriteLogService.addFavoriteLog(filePath);

        openUrl("/");

        WebElement fileTreeDiv = driver.findElement(By.id("file-tree"));

        List<WebElement> favorites = driver.findElementsByClassName("favorite-item");
        assert favorites.size() == 1;

        List<WebElement> favoritesIconInTree = driver.findElementsByClassName("in-favorites");
        assert favoritesIconInTree.size() == 1;

        WebElement emptyLogItem = fileTreeDiv.findElement(By.xpath("sl-fs-tree-item/sl-fs-tree-item//div[@class='fs-item fs-item-file']/div[@class='file-name'][text()='empty.log']/.."));
        assert emptyLogItem.findElement(By.className("in-favorites")).equals(favoritesIconInTree.get(0));

        favoritesIconInTree.get(0).click();

        Thread.sleep(200);

        assert favoriteLogService.getFavorites().isEmpty();

        driver.findElementById("no-favorites");

        noImplicitWait(() -> {
            assert driver.findElementsByClassName("favorite-item").isEmpty();
            assert emptyLogItem.findElements(By.className("in-favorites")).isEmpty();
        });

        Actions builder = new Actions(driver);
        builder.moveToElement(emptyLogItem, 0, 0).perform();

        emptyLogItem.findElement(By.className("fa-star")).click();

        Thread.sleep(200);

        assert driver.findElementsByClassName("favorite-item").size() == 1;
        assert emptyLogItem.findElements(By.className("in-favorites")).size() == 1;
        favoriteLogService.getFavorites().contains(filePath);
    }

    private void allowDatadirOnly() {
        accessManager().setAllowedPaths(Collections.singletonList(dataDir));
        ctx.getBean(LvFileNavigationManagerImpl.class).setDefaultDirectory(dataDir);
    }

    @Test
    public void testFavoritesItems() throws InterruptedException {
        allowDatadirOnly();

        favoriteLogService.addFavoriteLog(getDataFilePath("empty.log"));
        favoriteLogService.addFavoriteLog(getDataFilePath("folder/some-log.log"));

        openUrl("/");

        WebElement fileTreeDiv = driver.findElement(By.id("file-tree"));
        WebElement folderItem = getOnlyElement(fileTreeDiv.findElements(By.xpath("./sl-fs-tree-item/sl-fs-tree-item//div/div[@class='file-name'][text()='folder']/..")));
        folderItem.findElement(By.cssSelector(".fa-caret-right")).click();


        List<WebElement> favorites = driver.findElementsByClassName("favorite-item");
        assert favorites.size() == 2;
        assertEquals(getDataFilePath("empty.log"), favorites.get(0).findElement(By.xpath("a")).getText());

        List<WebElement> favoritesIconInTree = driver.findElementsByClassName("in-favorites");
        assert favoritesIconInTree.size() == 2;

        Actions builder = new Actions(driver);
        builder.moveToElement(favorites.get(0), 0, 0).perform();

        favorites.get(0).findElement(By.className("fa-times")).click();

        Thread.sleep(200);

        assert getOnlyElement(favoriteLogService.getFavorites()).equals(getDataFilePath("folder/some-log.log"));
        assertEquals(1, driver.findElementsByClassName("in-favorites").size());

        favorites = driver.findElementsByClassName("favorite-item");
        assertEquals(1, favorites.size());
        assertEquals(getDataFilePath("folder/some-log.log"), favorites.get(0).findElement(By.xpath("a")).getText());
    }

    @Test
    public void testExpandingSingleElementFolder() {
        allowDatadirOnly();

        openUrl("/");

        WebElement fileTreeDiv = driver.findElement(By.id("file-tree"));
        WebElement se1 = getOnlyElement(fileTreeDiv.findElements(By.xpath("sl-fs-tree-item/sl-fs-tree-item//div/div[@class='file-name'][text()='single-element-folder1']/..")));
        se1.findElement(By.cssSelector(".fa-caret-right")).click();
        fileTreeDiv.findElements(By.xpath("sl-fs-tree-item/sl-fs-tree-item//div[@class='file-name'][text()='log-in-sinle-element-folder.log']"));
    }

    @Test
    public void testVisibleByPattern() {
        accessManager().setAllowedPaths(Collections.singletonMap(dataDir, Pattern.compile("(empty|search)\\.log")));
        ctx.getBean(LvFileNavigationManagerImpl.class).setDefaultDirectory(dataDir);

        openUrl("/");

        WebElement fileTreeDiv = driver.findElement(By.id("file-tree"));

        getOnlyElement(fileTreeDiv.findElements(By.xpath("sl-fs-tree-item")));

        List<WebElement> items = fileTreeDiv.findElements(By.cssSelector(".fs-item-file .file-name"));
//        List<WebElement> items = fileTreeDiv.findElements(By.className("fs-item-file"));
        assertEquals(2, items.size());

        assertEquals(Sets.newHashSet("empty.log", "search.log"), items.stream().map(WebElement::getText).collect(Collectors.toSet()));
    }
}
