package com.logviewer.web;

import com.logviewer.AbstractLogTest;
import com.logviewer.files.FileTypes;
import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class LogNavigatorControllerTest extends AbstractLogTest {

    @Test
    public void testCustomExtension() {
        System.setProperty("log-viewer.navigation.log-file-pattern", ".*\\.properties");
        try {
            assertEquals(FileTypes.LOG.getTypeId(), getFileItem().type);
        } finally {
            System.clearProperty("log-viewer.navigation.log-file-pattern");
        }
    }

    @Test
    public void testPropertyExtension() {
        assertEquals(FileTypes.PROPS.getTypeId(), getFileItem().type);
    }

    private LogNavigatorController.FileItem getFileItem() {
        URL url = getClass().getResource("/test.properties");
        assert url.getProtocol().equals("file");

        Path dir = Paths.get(url.getPath()).getParent();

        ApplicationContext context = getCommonContext();

        LogNavigatorController controller = new LogNavigatorController();
        context.getAutowireCapableBeanFactory().autowireBeanProperties(controller, AutowireCapableBeanFactory.AUTOWIRE_NO, false);

        LogNavigatorController.RestContent content = controller.getDirContent(dir);
        LogNavigatorController.FsItem item = content.content.stream().filter(f -> f.name.equals("test.properties")).findFirst().get();
        return (LogNavigatorController.FileItem) item;
    }
}