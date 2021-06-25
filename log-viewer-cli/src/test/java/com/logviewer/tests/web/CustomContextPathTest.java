package com.logviewer.tests.web;

import org.junit.Test;
import org.openqa.selenium.By;

public class CustomContextPathTest extends AbstractWebTestCase {

    private static final String[] CONTEXT_PATHS = {"/", "/aaa", "/aaa/bbb"};
    private static final String[] SERVLET_PATHS = {"/*", "/sss/*", "/sss/hhh/*"};

    @Test
    public void testCustomPath() throws Exception {
        String logPath = getDataFilePath("1-7.log");

        try {
            for (String contextPath : CONTEXT_PATHS) {
                for (String servletPath : SERVLET_PATHS) {
                    System.setProperty("log-viewer.server.context-path", contextPath);
                    System.setProperty("log-viewer.server.servlet-path", servletPath);

                    try {
                        withNewServer(() -> {
                            String path = contextPath;
                            if (contextPath.endsWith("/"))
                                path = path.substring(0, path.length() - 1);

                            assert servletPath.startsWith("/");

                            path = path + servletPath;

                            assert servletPath.endsWith("/*");
                            path = path.substring(0, path.length() - "/*".length());

                            openUrl(path);

                            driver.findElement(By.cssSelector(".dir-content-panel"));

                            openUrl(path + '/');

                            driver.findElement(By.cssSelector(".dir-content-panel"));

                            openUrl(path + "/log", "log", logPath);
                            checkRecordCount(7);
                        });
                    } finally {
                        System.clearProperty("log-viewer.server.context-path");
                        System.clearProperty("log-viewer.server.servlet-path");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
