package com.logviewer.tests.web;

import com.google.common.base.Throwables;
import com.logviewer.LogViewerMain;
import com.logviewer.TestUtils;
import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.Log;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.data2.LogService;
import com.logviewer.impl.InmemoryFavoritesService;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.tests.pages.LogPage;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.TestListener;
import com.logviewer.utils.Utils;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.lang.NonNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public abstract class AbstractWebTestCase implements LogPage {

    private static final int TEST_PORT = 8333;

    public static final int WINDOW_WIDTH = 1000;

    protected static final int LINE_HEIGHT = 16;
    public static final int WAIT_FOR_TIMEOUT = 3000;

    public static RemoteWebDriver driver;

    protected static Path dataDir;
    protected static Path tmpDir;

    protected static AnnotationConfigApplicationContext ctx;
    private static Server server;

    private boolean expectError;

    @Autowired
    protected FavoriteLogService favoriteLogService;

    static {
        URL resource = AbstractWebTestCase.class.getResource("/integration/data/1-7.log");
        assert "file".equals(resource.getProtocol());
        dataDir = Paths.get(resource.getFile()).getParent();

        try {
            tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "log-viewer-integration-test-data");
            if (Files.isDirectory(tmpDir)) {
                Utils.deleteContent(tmpDir);
            }                                       
            else {
                Files.createDirectory(tmpDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void clearContext() throws Exception {
        if (server != null) {
            server.stop();
            ctx.close();
            server = null;
            ctx = null;
        }
    }

    private static void initContext() throws Exception {
        assert AbstractWebTestCase.ctx == null;
        assert server == null;

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(LvTestConfig.class, LogViewerServerConfig.class);
        ctx.refresh();
        LogContextHolder.setInstance(ctx);
        AbstractWebTestCase.ctx = ctx;

        server = new LogViewerMain().startup();
    }

    @BeforeClass
    public static void initSpring() throws Exception {
        if (server == null) {
            initContext();
        }

        InmemoryFavoritesService favoritesService = ctx.getBean(InmemoryFavoritesService.class);
        favoritesService.setEditable(true);
        favoritesService.clear();

        Log.setLogIdGenerator(path -> Paths.get(path).getFileName().toString());
    }

    protected static <E extends Throwable> void withNewServer(TestUtils.ExceptionalRunnable<E> run) throws Exception, E {
        clearContext();

        try {
            initContext();

            run.run();
        } finally {
            clearContext();
        }
    }

    @BeforeClass
    public static void initDriver() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (driver == null) {
            String driverFactoryClassName = System.getProperty("web.driver", "com.logviewer.tests.utils.ChromeDriverFactory");

            Class<?> driverFactoryClass = AbstractWebTestCase.class.getClassLoader().loadClass(driverFactoryClassName);
            Supplier<RemoteWebDriver> driverFactory = (Supplier<RemoteWebDriver>) driverFactoryClass.newInstance();

            driver = driverFactory.get();
            driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        }
    }

    @Before
    public final void init() {
        ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
        
        ctx.getBeansOfType(TestListener.class).values().forEach(TestListener::beforeTest);
    }

    protected void notExistWait(@NonNull By by) {
        waitFor(() -> noImplicitWait(() -> driver.findElements(by).isEmpty()));
    }

    protected void notExist(@NonNull By by) {
        noImplicitWait(() -> {
            assert driver.findElements(by).isEmpty();
        });
    }

    protected void notExist(@NonNull WebElement element, @NonNull By by) {
        noImplicitWait(() -> {
            assert element.findElements(by).isEmpty();
        });
    }

    protected void noImplicitWait(Runnable run) {
        noImplicitWait(Executors.callable(run));
    }

    protected static <T> T noImplicitWait(Callable<T> run) {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);

        try {
            return run.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        }
    }

    protected void waitForRecordsLoading() {
        driver.findElement(By.xpath("//div[@id='records']/div[@class='record']"));
    }

    protected void waitFor(Callable<Boolean> condition) {
        long stopWaitTime = System.currentTimeMillis() + WAIT_FOR_TIMEOUT;

        try {
            while (true) {
                try {
                    Boolean res = condition.call();
                    if (Boolean.TRUE.equals(res))
                        return;
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }

                if (System.currentTimeMillis() > stopWaitTime)
                    throw new IllegalStateException();

                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    protected static void openUrl(String path, String ... params) {
        Map<String, List<String>> map = new HashMap<>();

        assert (params.length & 1) == 0;
        for (int i = 0; i < params.length; i += 2) {
            String key = params[i];
            String value = params[i + 1];

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        
        openUrl(path, map);
    }

    protected static void openUrl(String path, @NonNull Map<String, List<String>> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://localhost:").append(TEST_PORT);

        if (!path.startsWith("/"))
            sb.append("/");

        sb.append(path);

        if (params.size() > 0) {
            sb.append('?');

            try {
                boolean first = true;
                for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                    String key = entry.getKey();

                    for (String value : entry.getValue()) {
                        if (first) {
                            first = false;
                        }
                        else {
                            sb.append('&');
                        }

                        sb.append(key).append('=').append(URLEncoder.encode(value, "utf-8"));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        driver.get(sb.toString());
    }

    protected static String getDataFilePath(String fileName) {
        URL resource = AbstractWebTestCase.class.getResource("/integration/data/" + fileName);
        if (resource == null)
            throw new IllegalArgumentException("File not found: " + fileName);

        assert "file".equals(resource.getProtocol());

        return resource.getFile();
    }

    protected static String openLog(String fileName) {
        String path = getDataFilePath(fileName);
        openUrl("log", "path", path);
        return path;
    }

    protected static void openLog(String ... fileName) {
        List<String> paths = Stream.of(fileName).map(AbstractWebTestCase::getDataFilePath).collect(Collectors.toList());

        openUrl("log", Collections.singletonMap("path", paths));
    }

    protected static void openLog(Path path) {
        openUrl("log", "path", path.toFile().getAbsolutePath());
    }

    protected void setHeight(int lineCount) {
        int overhead = driver.manage().window().getSize().height - driver.findElement(By.id("logPane")).getSize().height;
        driver.manage().window().setSize(new Dimension(WINDOW_WIDTH, overhead + lineCount * LINE_HEIGHT + 4));
    }

    protected static String join(Collection<WebElement> elements) {
        return join(elements, "");
    }

    protected static String join(Collection<WebElement> elements, String delimiter) {
        return elements.stream().map(e -> e.getAttribute("textContent")).collect(Collectors.joining(delimiter));
    }

    @Before
    public void before() {
        ctx.getBean(LogService.class).reset();

        ctx.getBean(LvFileAccessManagerImpl.class).allowAll();

        driver.manage().window().setSize(new Dimension(WINDOW_WIDTH, LINE_HEIGHT * 50));
    }

    @AfterClass
    public static void done() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }

    @After
    public void checkBrowserErrors() {
        if (!expectError) {
            for (LogEntry entry : driver.manage().logs().get("browser").getAll()) {
                if (entry.getLevel() == Level.SEVERE)
                    throw new RuntimeException("Browser log contains an error: " + entry.getMessage());
            }
        }
    }

    protected void expectError() {
        expectError = true;
    }

    protected String getClipboardText() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);
        if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor))
            return null;

        try {
            return (String) contents.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected WebElement recordByText(@NonNull String text) {
        text = text.replaceAll("\\s+", " ");
        return driver.findElement(By.xpath("//div[@id='records']/div[contains(@class,'record')][normalize-space(.)='" + text + "']"));
    }

    protected WebElement lastRecord() {
        return driver.findElement(By.xpath("//div[@id='records']/div[@class='record'][last()]"));
    }

    protected WebElement checkLastRecord(String text) {
        return driver.findElement(By.xpath("//div[@id='records']/div[@class='record'][last()]/div[@class='rec-text'][normalize-space(.)='" + text + "']"));
    }

    protected String getSelectedText() {
        return driver.executeScript("return window.getSelection().toString()").toString();
    }

    protected String copyPermalink() {
        driver.findElement(MENU).click();
        driver.findElement(By.id("copyPermalink")).click();

        closeSuccessAlert();
        driver.findElement(MENU).click();

        return getClipboardText();
    }

    protected String copySelection() {
        new Actions(driver).keyDown(Keys.CONTROL).sendKeys("c").keyUp(Keys.CONTROL).perform();

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        try {
            return (String) contents.getTransferData(DataFlavor.stringFlavor);
        } catch (IOException | UnsupportedFlavorException e) {
            throw new RuntimeException(e);
        }
    }

    private int parseCssSize(String cssValue) {
        assert cssValue.endsWith("px");
        return Integer.parseInt(cssValue.substring(0, cssValue.length() - 2));
    }

    protected void checkRecordViewPosition(WebElement record, int offset) {
        assertEquals(offset, getRecordViewPosition(record));
    }

    protected int getRecordViewPosition(WebElement record) {
        WebElement logPane = driver.findElement(By.id("logPane"));
        int logPanePosition = logPane.getLocation().y
                + 0// parseCssSize(logPane.getCssValue("padding-top"))
                + 0//parseCssSize(logPane.getCssValue("margin-top"))
                + 0;//parseCssSize(logPane.getCssValue("border-top-width"));

        return record.getLocation().y - logPanePosition;
    }

    protected void checkRecordCount(int number) {
        if (number == 0) {
            driver.findElement(By.cssSelector(".empty-log-message .no-record-msg"));
        } else {
            waitFor(() -> getRecord().size() == number);
        }
    }

    protected void select(WebElement element) {
        driver.executeScript("" +
                "var range = document.createRange(); " +
                "range.selectNode(arguments[0]);" +
                "window.getSelection().removeAllRanges();" +
                "window.getSelection().addRange(range);", element);
    }

    protected List<WebElement> getRecord() {
        return driver.findElements(By.cssSelector("#records > .record"));
    }

    protected String getVisibleRecords() {
        WebElement logPane = driver.findElement(By.id("logPane"));
        int logPaneInternalHeight = logPane.getSize().height
                - parseCssSize(logPane.getCssValue("padding-top"))
                - parseCssSize(logPane.getCssValue("margin-top"))
                - parseCssSize(logPane.getCssValue("border-top-width"))
                - parseCssSize(logPane.getCssValue("padding-bottom"))
                - parseCssSize(logPane.getCssValue("margin-bottom"))
                - parseCssSize(logPane.getCssValue("border-bottom-width"));

        return driver.findElements(By.cssSelector("#records > .record > .rec-text")).stream()
                .filter(r -> {
                    int pos = getRecordViewPosition(r);
                    return pos >= 0 && pos < logPaneInternalHeight - LINE_HEIGHT;
                }).map(WebElement::getText)
                .collect(Collectors.joining("\n"));
    }

    protected void closeSuccessAlert() {
        closeAlert("toast-success");
    }

    protected void closeInfoAlert() {
        closeAlert("toast-info");
    }

    protected void closeAlert(String classname) {
        driver.findElement(By.cssSelector("." + classname + ".ngx-toastr")).click();

        waitFor(() -> {
            return noImplicitWait(() -> driver.findElements(By.cssSelector(".toast-success.ngx-toastr")).isEmpty());
        });
    }

    private static DateFormat[] FORMATS = new DateFormat[]{
            new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS"),
            new SimpleDateFormat("yyyyMMdd HH:mm:ss"),
            new SimpleDateFormat("yyyyMMdd HH:mm"),
    };

    public static long date(@NonNull String date) {
        for (DateFormat format : FORMATS) {
            try {
                return format.parse(date).getTime() * 1000_000;
            } catch (ParseException ignored) {

            }
        }

        throw new IllegalArgumentException("Invalid date format: " + date);
    }

    protected static void setValue(@NonNull WebElement element, @NonNull String value) {
        driver.executeScript("arguments[0].value='" + value.replaceAll("['\\\\]", "\\\\$0").replace("\n", "\\n") + "';", element);
    }

    protected static WebElement assertDisabled(By element) {
        return assertDisabled(driver.findElement(element));
    }

    protected static WebElement assertDisabled(WebElement element) {
        assertEquals("true", element.getAttribute("disabled"));
        return element;
    }

    protected static WebElement assertEnabled(WebElement element) {
        assertNull(element.getAttribute("disabled"));
        return element;
    }

    protected static WebElement assertEnabled(By element) {
        return assertEnabled(driver.findElement(element));
    }
}
