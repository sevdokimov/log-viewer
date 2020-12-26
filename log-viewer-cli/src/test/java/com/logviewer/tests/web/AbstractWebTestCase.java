package com.logviewer.tests.web;

import com.google.common.base.Throwables;
import com.logviewer.LogViewerMain;
import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.data2.LogService;
import com.logviewer.impl.InmemoryFavoritesService;
import com.logviewer.services.LvFileAccessManagerImpl;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.TestListener;
import com.logviewer.utils.Utils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.lang.NonNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public abstract class AbstractWebTestCase {

    private static final int TEST_PORT = 8333;

    public static final int WINDOW_WIDTH = 1000;

    protected static final int LINE_HEIGHT = 16;
    public static final int WAIT_FOR_TIMEOUT = 3000;

    protected static RemoteWebDriver driver;

    protected static Path dataDir;
    protected static Path tmpDir;

    protected static LogService testLogService;

    protected static ConfigurableApplicationContext ctx;

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

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (ctx == null) {
            ctx = new AnnotationConfigApplicationContext(LvTestConfig.class, LogViewerServerConfig.class);

            testLogService = ctx.getBean(LogService.class);

            LogContextHolder.setInstance(ctx);
        }

        InmemoryFavoritesService favoritesService = ctx.getBean(InmemoryFavoritesService.class);
        favoritesService.setEditable(true);
        favoritesService.clear();

        startJettyIfNeed();

        String driverFactoryClassName = System.getProperty("web.driver", "com.logviewer.tests.utils.ChromeDriverFactory");

        Class<?> driverFactoryClass = AbstractWebTestCase.class.getClassLoader().loadClass(driverFactoryClassName);
        Supplier<RemoteWebDriver> driverFactory = (Supplier<RemoteWebDriver>) driverFactoryClass.newInstance();

        driver = driverFactory.get();
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    private static void startJettyIfNeed() throws Exception {
        try {
            Socket s = new Socket("localhost", TEST_PORT);
            s.close();

            return;
        } catch (ConnectException ignored) {
            // Jetty is not started
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new LogViewerMain().startup();
    }

    @Before
    public final void init() {
        ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
        
        ctx.getBeansOfType(TestListener.class).values().forEach(TestListener::beforeTest);
    }

    protected void notExist(@NonNull By by) {
        noImplicitWait(() -> {
            assert driver.findElements(by).isEmpty();
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
        sb.append("http://localhost:").append(TEST_PORT).append("/");

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
        int overhead = driver.manage().window().getSize().height - driver.findElementById("logPane").getSize().height;
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
        testLogService.reset();

        ctx.getBean(LvFileAccessManagerImpl.class).allowAll();

        driver.manage().window().setSize(new Dimension(WINDOW_WIDTH, LINE_HEIGHT * 50));
    }

    @AfterClass
    public static void done() {
        if (driver != null)
            driver.close();
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

    protected void filterState(int index, boolean enabled) {
        driver.findElementById("filters-dd").click();

        WebElement filterCheckbox = driver.findElementByCssSelector(".filter-table > tr:nth-child(" + (index + 1) + ") > td:first-child > input");
        if (enabled != Boolean.parseBoolean(filterCheckbox.getAttribute("checked")))
            driver.executeScript("arguments[0].click()", filterCheckbox);
        
        driver.executeScript("arguments[0].click()", driver.findElementByXPath("//sl-filter-panel/form/button[@type='submit']"));
    }

    protected WebElement recordByText(@NonNull String text) {
        text = text.replaceAll("\\s+", " ");
        return driver.findElement(By.xpath("//div[@id='records']/div[@class='record'][normalize-space(.)='" + text + "']"));
    }

    protected WebElement lastRecord() {
        return driver.findElement(By.xpath("//div[@id='records']/div[@class='record'][last()]"));
    }

    protected WebElement checkLastRecord(String text) {
        return driver.findElement(By.xpath("//div[@id='records']/div[@class='record'][last()]/div[@class='rec-text'][text()='" + text + "']"));
    }

    protected String getSelectedText() {
        return driver.executeScript("return window.getSelection().toString()").toString();
    }

    protected String copyPermalink() {
        driver.findElement(By.id("menu-icon")).click();
        driver.findElementById("copyPermalink").click();

        closeSuccessAlert();
        driver.findElement(By.id("menu-icon")).click();

        return getClipboardText();
    }

    private int parseCssSize(String cssValue) {
        assert cssValue.endsWith("px");
        return Integer.parseInt(cssValue.substring(0, cssValue.length() - 2));
    }

    protected void checkRecordViewPosition(WebElement record, int offset) {
        assertEquals(offset, getRecordViewPosition(record));
    }

    protected int getRecordViewPosition(WebElement record) {
        WebElement logPane = driver.findElementById("logPane");
        int logPanePosition = logPane.getLocation().y
                + 0// parseCssSize(logPane.getCssValue("padding-top"))
                + 0//parseCssSize(logPane.getCssValue("margin-top"))
                + 0;//parseCssSize(logPane.getCssValue("border-top-width"));

        return record.getLocation().y - logPanePosition;
    }

    protected List<WebElement> getRecord() {
        return driver.findElementsByCssSelector("#records > .record");
    }

    protected String getVisibleRecords() {
        WebElement logPane = driver.findElementById("logPane");
        int logPaneInternalHeight = logPane.getSize().height
                - parseCssSize(logPane.getCssValue("padding-top"))
                - parseCssSize(logPane.getCssValue("margin-top"))
                - parseCssSize(logPane.getCssValue("border-top-width"))
                - parseCssSize(logPane.getCssValue("padding-bottom"))
                - parseCssSize(logPane.getCssValue("margin-bottom"))
                - parseCssSize(logPane.getCssValue("border-bottom-width"));

        return driver.findElementsByCssSelector("#records > .record > .rec-text").stream()
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
        driver.findElementByCssSelector("." + classname + ".ngx-toastr").click();

        waitFor(() -> {
            return noImplicitWait(() -> driver.findElementsByCssSelector(".toast-success.ngx-toastr").isEmpty());
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
                return format.parse(date).getTime();
            } catch (ParseException ignored) {

            }
        }

        throw new IllegalArgumentException("Invalid date format: " + date);
    }
}
