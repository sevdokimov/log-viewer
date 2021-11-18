package com.logviewer.tests.web;

import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.services.PathPattern;
import com.logviewer.tests.pages.ChooserPage;
import com.logviewer.web.LogNavigatorController;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.logviewer.tests.pages.ChooserPage.findFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FsNavigationTest extends AbstractWebTestCase implements ChooserPage {

    public static final List<String> ALL_ROOT_FILES = Arrays.asList("aaa", "bbb", "case-match", "fff", "search", "r.log", "r2.log");

    public static final By EDIT_PATH_ICON = By.cssSelector(".current-path .edit-icon");
    public static final By PATH_EDITOR_FOCUSED = By.cssSelector(".current-path-edited input[type=\"text\"]:focus");
    public static final By INPUT_PATH = By.cssSelector(".current-path-edited input[type=\"text\"]:focus");

    protected static LvFileAccessManagerImpl accessManager() {
        return ctx.getBean(LvFileAccessManagerImpl.class);
    }

    @Test
    public void disableFileTree() {
        System.setProperty(LogNavigatorController.CFG_FS_NAVIGATION_ENABLED, "false");

        try {
            openUrl("/");

            driver.findElement(By.xpath("//h4[text()='Favorites']"));

            notExist(By.className("dir-content-panel"));
        } finally {
            System.clearProperty(LogNavigatorController.CFG_FS_NAVIGATION_ENABLED);
        }
    }

    @Test
    public void testFileTreeFavorites() {
        allowDatadirOnly();

        String filePath = getDataFilePath("empty.log");

        favoriteLogService.addFavoriteLog(filePath);

        openUrl("/");

        List<WebElement> favorites = driver.findElementsByClassName("favorite-item");
        assert favorites.size() == 1;
    }

    @Test
    public void testVisibleByPattern() {
        PathPattern d = new PathPattern(dataDir, p -> p.toString().matches("(empty|search)\\.log"), dir -> false);
        accessManager().setPaths(Collections.singletonList(d));

        openUrl("/");

        assertEquals(dataDir.toString(), currentPath());

        assertEquals(Arrays.asList("empty.log", "search.log"), fileNames());
    }

    @Test
    public void selectUserDirByDefault() {
        openUrl("/");

        assertEquals(System.getProperty("user.home"), currentPath());
    }

    @Test
    public void manyRoots() {
        Path root = navigationRoot();
        accessManager().setPaths(Arrays.asList(
                PathPattern.directory(root.resolve("aaa")),
                PathPattern.directory(root.resolve("bbb"))));

        openUrl("/");

        List<String> fileNames = fileNames();

        assertTrue(fileNames.stream().anyMatch(name -> name.endsWith("/navigation-root/aaa")));
        assertTrue(fileNames.stream().anyMatch(name -> name.endsWith("/navigation-root/bbb")));
        assert fileNames.size() == 2;

        assert selectedFile().getText().endsWith("/aaa");

        int firstItemY = selectedFile().getLocation().y;

        new Actions(driver).doubleClick(selectedFile()).perform();

        checkSelectedFile("a.log");
        assertEquals(Arrays.asList("a.log", "a2.log"), fileNames());
        assertEquals(root.resolve("aaa").toString(), currentPath());

        assertEquals(firstItemY, selectedFile().getLocation().y);
    }

    @Test
    public void navigationMouse() {
        Path root = navigationRoot();
        accessManager().setPaths(Collections.singletonList(PathPattern.directory(root)));

        openUrl("/");

        assertEquals(root.toString(), currentPath());

        checkSelectedFile("aaa");
        assertEquals(ALL_ROOT_FILES, fileNames());

        // Check selection
        findFile("r.log").click();
        checkSelectedFile("r.log");

        // Check navigation by file click
        new Actions(driver).doubleClick(findFile("bbb")).perform();

        checkSelectedFile("b.log");
        assertEquals(root.resolve("bbb").toString(), currentPath());
        assertEquals(Arrays.asList("b.log", "b2.log"), fileNames());


        // Check navigation by head click
        clickNavigation("navigation-root");

        checkSelectedFile("bbb");
        assertEquals(ALL_ROOT_FILES, fileNames());
    }

    private void clickNavigation(@NonNull String pathPart) {
        List<WebElement> navigationRoots = driver.findElementsByXPath("//div[@class='current-path']/span[@class='path-item']/a[text()='" + pathPart + "']");
        navigationRoots.get(navigationRoots.size() - 1).click();
    }

    @Test
    public void openLogMouse() {
        openPage();

        new Actions(driver).doubleClick(findFile("r.log")).perform();

        assertEquals("r1\nr2\nr3", getVisibleRecords());
    }

    @Test
    public void keyboardNavigation() {
        openPage();

        checkSelectedFile("aaa");

        new Actions(driver).sendKeys(Keys.ENTER).perform();

        checkSelectedFile("a.log");
        new Actions(driver).sendKeys(Keys.UP).perform();
        checkSelectedFile("a.log");
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("a2.log");
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("a2.log");
        new Actions(driver).sendKeys(Keys.BACK_SPACE).perform();
        checkSelectedFile("aaa");
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("bbb");
        new Actions(driver).sendKeys(Keys.ENTER).perform();
        checkSelectedFile("b.log");

        clickNavigation("navigation-root");
        checkSelectedFile("bbb");
        new Actions(driver).sendKeys(Keys.UP).perform(); // check that focus was not lost
        checkSelectedFile("aaa");
    }

    @Test
    public void startSearchOnType() {
        openPage();

        new Actions(driver).doubleClick(findFile("search")).perform();

        assertEquals(6, fileNames().size()); // navigation-root/search contains 6 files

        new Actions(driver).sendKeys("f").perform();

        assertEquals(Arrays.asList("fff.log", "fff1.log"), fileNames());
        WebElement filterInput = driver.findElement(By.cssSelector(".search-pane .filter-input"));
        assertEquals("f", filterInput.getAttribute("value"));
    }

    @Test
    public void sizeAndModificationDate() throws IOException {
        openPage();

        WebElement name = findFile("r2.log");
        List<WebElement> td = name.findElements(By.xpath("../td"));

        assertThat(td.get(2).getText(), matchesPattern("0\\b.*"));
        assertThat(td.get(3).getText(), matchesPattern("\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d"));

        name = findFile("r.log");
        td = name.findElements(By.xpath("../td"));

        URL fileUrl = getClass().getResource("/integration/navigation-root/r.log");
        int fileSize = fileUrl.openConnection().getContentLength();

        assertThat(td.get(2).getText(), matchesPattern(fileSize + "\\b.*"));
        assertThat(td.get(3).getText(), matchesPattern("\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d"));
    }

    @Test
    public void clearSearchAfterNavigation() {
        openPage();

        driver.findElement(By.cssSelector(".search-pane .tool-button")).click();
        WebElement filterInput = driver.findElement(By.cssSelector(".search-pane .filter-input"));
        new Actions(driver).sendKeys("searc").perform();

        assertEquals(Collections.singletonList("search"), fileNames());
        new Actions(driver).sendKeys(Keys.ENTER).perform();

        notExist(By.cssSelector(".search-pane .tool-button.tool-button-pressed"));
        assert !filterInput.isDisplayed();

        assertEquals(6, fileNames().size());

        driver.findElement(By.cssSelector(".search-pane .tool-button")).click();
        filterInput.isDisplayed();
        new Actions(driver).sendKeys("aaa").perform();

        clickNavigation("navigation-root");
        notExist(By.cssSelector(".search-pane .tool-button.tool-button-pressed"));
        assert !filterInput.isDisplayed();
    }

    @Test
    public void closeSearchOnEscape() {
        openPage();

        new Actions(driver).doubleClick(findFile("search")).perform();
        driver.findElement(By.cssSelector(".search-pane .tool-button")).click();
        WebElement filterInput = driver.findElement(By.cssSelector(".search-pane .filter-input"));
        new Actions(driver).sendKeys("ab").perform();

        assertEquals(Arrays.asList("ab.log", "abc.log", "abc1.log"), fileNames());

        new Actions(driver).sendKeys(Keys.ESCAPE).perform();

        notExist(By.cssSelector(".search-pane .tool-button.tool-button-pressed"));
        assert !filterInput.isDisplayed();

        assertEquals(6, fileNames().size());

        notExist(By.cssSelector(".file-list .name .occurrence"));

        checkSelectedFile("ab.log");

        // Check focus is on the right element
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("abc.log");
    }

    @Test
    public void navigationAndSearch() {
        openPage();

        new Actions(driver).doubleClick(findFile("search")).perform();

        driver.findElement(By.cssSelector(".search-pane .tool-button")).click();
        driver.findElement(By.cssSelector(".search-pane .filter-input"));
        new Actions(driver).sendKeys("ab").perform(); // check that focus was not lost

        assertEquals(Arrays.asList("ab.log", "abc.log", "abc1.log"), fileNames());
        assertEquals(Collections.nCopies(3, "ab"), driver.findElementsByCssSelector(".file-list .name .occurrence").stream()
                .map(WebElement::getText).collect(Collectors.toList()));

        checkSelectedFile("ab.log");
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("abc.log");

        new Actions(driver).sendKeys(Keys.BACK_SPACE).perform();

        assertEquals("a", driver.findElement(By.cssSelector(".search-pane .filter-input")).getAttribute("value"));

        assertEquals(Collections.nCopies(6, "a"), driver.findElementsByCssSelector(".file-list .name .occurrence").stream()
                .map(WebElement::getText).collect(Collectors.toList()));

        assertEquals(Arrays.asList("aaa.log", "ab.log", "abc.log", "abc1.log"), fileNames());
        checkSelectedFile("abc.log");

        // don't lost focus after empty search result
        new Actions(driver).sendKeys("G").perform();
        noImplicitWait(() -> assertEquals(Collections.emptyList(), fileNames()));

        new Actions(driver).sendKeys(Keys.BACK_SPACE).perform();

        checkSelectedFile("abc.log");

        // don't lost focus after click to a file
        findFile("ab.log").click();
        checkSelectedFile("ab.log");
        driver.findElement(By.cssSelector(".search-pane .filter-input:focus"));
    }

    private void openPage() {
        Path root = navigationRoot();
        accessManager().setPaths(Collections.singletonList(PathPattern.directory(root)));

        openUrl("/");
    }

    @Test
    public void caseMatch() {
        openPage();
        new Actions(driver).doubleClick(findFile("case-match")).perform();

        assertEquals(Arrays.asList("aaa1.log", "AAA2.log", "z1.log", "z2.log", "z3.log", "z10.log"), fileNames());

        new Actions(driver).sendKeys("aAa").perform();

        assertEquals(Arrays.asList("aaa1.log", "AAA2.log"), fileNames());
        assertEquals("aaaAAA", driver.findElementsByCssSelector(".file-list .name .occurrence").stream()
                .map(WebElement::getText).collect(Collectors.joining()));
    }

    @Test
    public void openFilterPanel() throws InterruptedException {
        openPage();

        WebElement searchToggler = driver.findElement(By.cssSelector(".search-pane .tool-button"));
        notExist(By.cssSelector(".search-pane .tool-button.tool-button-pressed"));

        searchToggler.click();

        driver.findElementsByCssSelector(".search-pane .tool-button.tool-button-pressed");

        WebElement filterInput = driver.findElement(By.cssSelector(".search-pane .filter-input:focus"));

        searchToggler.click();

        Thread.sleep(20);

        notExist(By.cssSelector(".search-pane .tool-button.tool-button-pressed"));

        assert !filterInput.isDisplayed();
    }

    @Test
    public void openingClosingPathEditor() {
        openPage();

        assertThat(currentPath(), is(navigationRoot().toString()));

        // Start edit and cancel by Escape
        driver.findElement(EDIT_PATH_ICON).click();

        notExist(EDIT_PATH_ICON);

        WebElement input = driver.findElement(By.cssSelector(".current-path-edited input[type=\"text\"]:focus"));

        assertThat(input.getAttribute("value"), is(navigationRoot().toString()));
        assertThat(input.getAttribute("selectionEnd"), is(String.valueOf(navigationRoot().toString().length())));
        assertThat(input.getAttribute("selectionStart"), is("0"));

        new Actions(driver).sendKeys(Keys.ESCAPE).perform();

        // Test that keyboard navigation still works
        checkSelectedFile("aaa");
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("bbb");
        new Actions(driver).sendKeys(Keys.UP).perform();
        checkSelectedFile("aaa");

        driver.findElement(EDIT_PATH_ICON);
        notExist(By.cssSelector(".current-path-edited"));

        // Start edit and cancel by Cancel button
        driver.findElement(EDIT_PATH_ICON).click();

        new Actions(driver).sendKeys("/abc").perform();

        assertThat(driver.findElement(PATH_EDITOR_FOCUSED).getAttribute("value"), is("/abc"));

        driver.findElement(By.cssSelector(".current-path-edited button[name=\"cancel-button\"]")).click();

        driver.findElement(EDIT_PATH_ICON);
        assertThat(currentPath(), is(navigationRoot().toString()));

        // Test that keyboard navigation still works
        checkSelectedFile("aaa");
        new Actions(driver).sendKeys(Keys.DOWN).perform();
        checkSelectedFile("bbb");
    }

    @Test
    public void selectionOnFilteringStart() {
        openPage();

        checkSelectedFile("aaa");

        new Actions(driver).sendKeys("f").perform();

        checkSelectedFile("fff");
    }

    @Test
    public void selectFileByTypePath() {
        openPage();

        // Open file
        driver.findElement(EDIT_PATH_ICON).click();

        setValue(driver.findElement(INPUT_PATH), "");
        new Actions(driver)
                .sendKeys(driver.findElement(INPUT_PATH), navigationRoot().toString() + "/aaa/a2.log")
                .sendKeys(Keys.ENTER)
                .perform();

        checkSelectedFile("a2.log");
        assertThat(currentPath(), is(navigationRoot().toString() + "/aaa"));

        // Open directory
        driver.findElement(EDIT_PATH_ICON).click();

        setValue(driver.findElement(INPUT_PATH), "");
        new Actions(driver)
                .sendKeys(driver.findElement(INPUT_PATH), navigationRoot().toString() + "/bbb")
                .sendKeys(Keys.ENTER)
                .perform();

        checkSelectedFile("b.log");
        assertThat(currentPath(), is(navigationRoot().toString() + "/bbb"));

        new Actions(driver).sendKeys(Keys.DOWN).perform(); // check that navigation still works
        checkSelectedFile("b2.log");

        // Open unexisting directory
        driver.findElement(EDIT_PATH_ICON).click();

        setValue(driver.findElement(INPUT_PATH), "");
        new Actions(driver)
                .sendKeys(driver.findElement(INPUT_PATH), navigationRoot().toString() + "/zxcbvbvcb")
                .sendKeys(Keys.ENTER)
                .perform();

        assertThat(currentPath(), is(navigationRoot().toString() + "/zxcbvbvcb"));

        assertThat(driver.findElement(By.cssSelector(".dir-content-panel .alert-danger")).getText().trim(), is("Directory not found"));

        // Open unaccessible directory
        driver.findElement(EDIT_PATH_ICON).click();

        setValue(driver.findElement(INPUT_PATH), "");
        new Actions(driver)
                .sendKeys(driver.findElement(INPUT_PATH), navigationRoot().getParent().toString() + "/zzz")
                .sendKeys(Keys.ENTER)
                .perform();

        assertThat(currentPath(), is(navigationRoot().getParent().toString() + "/zzz"));

        assertThat(driver.findElement(By.cssSelector(".dir-content-panel .alert-danger")).getText().trim(), containsString("not accessible"));
    }

    @Test
    public void saveStateInUrl() {
        openPage();

        findFile("aaa"); // wait for initialization

        assertThat(driver.getCurrentUrl(), not(containsString("dir="))); // don't add "dir=" param when the default directory is opened

        new Actions(driver).doubleClick(findFile("aaa")).perform();

        waitFor(() -> fileNames().equals(Arrays.asList("a.log", "a2.log")));

        assertThat(driver.getCurrentUrl(), containsString("dir="));
        assertThat(driver.getCurrentUrl(), containsString("%2Faaa"));

        driver.navigate().refresh();

        waitFor(() -> fileNames().equals(Arrays.asList("a.log", "a2.log")));

        assertThat(driver.getCurrentUrl(), containsString("dir="));

        new Actions(driver).sendKeys(Keys.BACK_SPACE).perform();

        findFile("aaa");

        assertThat(driver.getCurrentUrl(), not(containsString("dir="))); // don't add "dir=" param when the default directory is opened
    }

    private WebElement checkSelectedFile(String expectedSelectedFile) {
        return driver.findElement(By.xpath("//table[contains(@class,'file-list')]//tr[contains(@class, 'selected')]/td[@class='name'][normalize-space(.)='" + expectedSelectedFile + "']"));
    }

    private WebElement selectedFile() {
        return driver.findElement(By.xpath("//table[contains(@class,'file-list')]//tr[contains(@class, 'selected')]/td[@class='name']"));
    }

    private Path navigationRoot() {
        try {
            return dataDir.getParent().resolve("navigation-root").toRealPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void allowDatadirOnly() {
        accessManager().setPaths(Collections.singletonList(PathPattern.directory(dataDir)));
    }

    private List<String> fileNames() {
        return driver.findElementsByCssSelector(".file-list .file-list-item .name").stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    private String currentPath() {
        return driver.findElement(By.className("current-path")).getText().replaceAll("\\s+", "");
    }
}