package com.logviewer;

import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.*;
import com.logviewer.data2.net.Node;
import com.logviewer.utils.Utils;
import org.junit.After;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractLogTest {

    public static final int TEST_SERVER_PORT = 9510;

    public static final Node NODE = new Node("localhost", TEST_SERVER_PORT);

    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    private final List<Path> tempDirectories = new ArrayList<>();
    private final List<Path> tempFiles = new ArrayList<>();

    private ConfigurableApplicationContext commonContext;

    @After
    public void destroyContexts() {
        contexts.forEach(Utils::closeQuietly);
        contexts.clear();

        for (Path tempDirectory : tempDirectories) {
            try {
                Utils.deleteContent(tempDirectory);
                Files.delete(tempDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tempDirectories.clear();

        for (Path tempFile : tempFiles) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tempFiles.clear();
    }

    protected synchronized ApplicationContext getCommonContext() {
        if (commonContext == null) {
            commonContext = createContext(LvTestConfig.class);
        }

        return commonContext;
    }

    protected synchronized ConfigurableApplicationContext createContext(Class<?>... annotatedClasses) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(annotatedClasses);
        contexts.add(ctx);
        return ctx;
    }

    protected synchronized LogService createLogServiceWithContext(Class<?>... annotatedClasses) {
        return createContext(annotatedClasses).getBean(LogService.class);
    }

    protected synchronized LogService getLogService() {
        if (contexts.isEmpty())
            getCommonContext();

        if (contexts.size() > 1)
            throw new RuntimeException("Ambiguous spring context");

        return contexts.get(0).getBean(LogService.class);
    }

    private String testClassName() {
        String simpleName = getClass().getSimpleName();

        if (simpleName.endsWith("Test")) {
            simpleName = simpleName.substring(0, simpleName.length() - "Test".length());
        }

        return simpleName;
    }

    public Snapshot log(String fileName, LogFormat format) {
        try {
            Path path = Paths.get(TestUtils.class.getResource(fileName).toURI());
            return log(path, format);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Snapshot log(Path path, LogFormat format) {
        Log log = getLogService().openLog(path.toString(), format);
        return log.createSnapshot();
    }

    protected String getTestLog(String relativePath) {
        String testFilePath = "/testdata/" + relativePath;
        URL url = AbstractLogTest.class.getResource(testFilePath);

        if (url == null)
            throw new IllegalStateException("Faile to find test data file: " + testFilePath);

        return url.getFile();
    }

    protected String getTestClassLog() {
        String testFilePath = "/testdata/" + testClassName() + ".log";
        URL url = AbstractLogTest.class.getResource(testFilePath);

        if (url == null) {
            throw new IllegalStateException("Faile to find test data file: " + testFilePath);
        }

        return url.getFile();
    }

    public List<Record> loadLog(String path, LogFormat logFormat) throws IOException, LogCrashedException {
        String file = getTestLog(path);

        Log log = getLogService().openLog(file, logFormat);

        List<Record> records = new ArrayList<>();
        try (Snapshot snapshot = log.createSnapshot()) {
            snapshot.processRecords(0, record -> {
                records.add(record);
                return true;
            });
        }

        return records;
    }

    /**
     * Creates temp directory and registers it in {@link #tempDirectories}. The directory will be deleted automatically
     * after finish test.
     */
    protected Path createTempDirectory() throws IOException {
        Path res = Files.createTempDirectory("log-test-");
        tempDirectories.add(res);
        return res;
    }

    /**
     * Creates temp file and registers it in {@link #tempFiles}. The file will be deleted automatically
     * after finish test.
     */
    protected Path createTempFile() throws IOException {
        Path res = Files.createTempFile("log-test-", ".log");
        tempFiles.add(res);
        return res;
    }

    protected ApplicationContext findContext(@NonNull LogService logService) {
        for (ConfigurableApplicationContext context : contexts) {
            if (context.getBean(LogService.class) == logService)
                return context;
        }

        return null;
    }

    protected void doRemoteTest(RemoteTest test) throws Exception {
        doRemoteTest(LvTestConfig.class, test);
    }

    protected void doRemoteTest(@NonNull Class<?> cfg, @NonNull RemoteTest test) throws Exception {
        LogService local = createLogServiceWithContext(cfg);
        LogService remote = createLogServiceWithContext(cfg, LogViewerServerConfig.class);

        test.doTest(local, remote);
    }

    protected String fieldValue(LogFormat format, Record record, String fieldName) {
        int fieldIdx = -1;

        LogFormat.FieldDescriptor[] fields = format.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].name().equals(fieldName)) {
                fieldIdx = i;
                break;
            }
        }

        assert fieldIdx >= 0 : fieldName;
        return record.getFieldText(fieldIdx);
    }

    protected Record read(@NonNull LogFormat logFormat, @NonNull String s) {
        LogReader reader = logFormat.createReader();
        boolean isSuccess = reader.parseRecord(new BufferedFile.Line(s));
        assert isSuccess : "Failed to parse: " + s;

        return reader.buildRecord();
    }

    protected void checkFields(Record record, String ... expectedFields) {
        assertEquals(expectedFields.length, record.getFieldsCount());

        for (int i = 0; i < expectedFields.length; i++) {
            String expected = expectedFields[i];
            String actual = record.getFieldText(i);

            if (expected.startsWith("~")) {
                assertTrue(actual, Pattern.compile(expected.substring(1)).matcher(actual).matches());
            } else {
                assertEquals(expected, actual);
            }
        }
    }

    public interface RemoteTest {
        void doTest(LogService local, LogService remote) throws Exception;
    }
}
