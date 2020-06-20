package com.logviewer;

import com.logviewer.api.LvFileNavigationManager;
import com.logviewer.data2.DirectoryNotVisibleException;
import com.logviewer.data2.LogService;
import com.logviewer.data2.Snapshot;
import com.logviewer.impl.LvFileAccessManagerImpl;
import com.logviewer.impl.LvFileNavigationManagerImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Convert2MethodRef")
public class FileManagerTest extends AbstractLogTest {

    @Test
    public void testDefaultDirectory() {
        LvFileNavigationManagerImpl manager = getCommonContext().getBean(LvFileNavigationManagerImpl.class);

        assertEquals(Paths.get(System.getProperty("user.home")), manager.getDefaultDirectory());

        manager.setDefaultDirectory(null);

        assertNull(manager.getDefaultDirectory());

        Path defDir = Paths.get(System.getProperty("user.home"), "logs");

        manager.setDefaultDirectory(defDir);

        assertEquals(defDir, manager.getDefaultDirectory());
    }

    @Test
    public void testList() throws IOException {
        LvFileNavigationManagerImpl manager = getCommonContext().getBean(LvFileNavigationManagerImpl.class);

        List<LvFileNavigationManager.LvFsItem> roots = manager.getChildren(null);
        assert roots.size() > 0;
        assert roots.stream().allMatch(p -> p.getPath().getParent() == null);

        Path tmpDir = createTempDirectory();

        Path subdir1 = tmpDir.resolve("subdir1");
        Path subdir2 = tmpDir.resolve("subdir2");

        Files.createDirectory(subdir1);
        Files.createDirectory(subdir2);

        Path aLog = subdir1.resolve("a.log");
        Path bLog = subdir1.resolve("b.log");
        Files.createFile(aLog);
        Files.createFile(bLog);

        Path log2 = subdir2.resolve("2.log");
        Files.createFile(log2);

        TestUtils.assertEqualsUnorder(manager.getChildren(tmpDir), f -> f.getPath(), subdir1, subdir2);
        TestUtils.assertEqualsUnorder(manager.getChildren(subdir1), f -> f.getPath(), aLog, bLog);
        TestUtils.assertEqualsUnorder(manager.getChildren(subdir2), f -> f.getPath(), log2);

        LvFileAccessManagerImpl accessManager = getCommonContext().getBean(LvFileAccessManagerImpl.class);

        accessManager.setAllowedPaths(Collections.singletonList(subdir2));

        TestUtils.assertEqualsUnorder(manager.getChildren(tmpDir), f -> f.getPath(), subdir2);
        TestUtils.assertEqualsUnorder(manager.getChildren(tmpDir.getParent()), f -> f.getPath(), tmpDir);
        assertNull(manager.getChildren(subdir1));
        TestUtils.assertEqualsUnorder(manager.getChildren(subdir2), f -> f.getPath(), log2);

        try (Snapshot snapshot = getLogService().openLog(aLog.toString(), LogService.DEFAULT_FORMAT).createSnapshot()) {
            assert snapshot.getError() instanceof DirectoryNotVisibleException;
        }

        try (Snapshot snapshot = getLogService().openLog(log2.toString(), LogService.DEFAULT_FORMAT).createSnapshot()) {
            assertNull(snapshot.getError());
        }

        accessManager.setAllowedPaths(Collections.singletonMap(subdir1, Pattern.compile("a.*").asPredicate()));

        TestUtils.assertEqualsUnorder(manager.getChildren(tmpDir), f -> f.getPath(), subdir1);
        TestUtils.assertEqualsUnorder(manager.getChildren(tmpDir.getParent()), f -> f.getPath(), tmpDir);
        assertNull(manager.getChildren(subdir2));
        TestUtils.assertEqualsUnorder(manager.getChildren(subdir1), f -> f.getPath(), aLog);

        try (Snapshot snapshot = getLogService().openLog(bLog.toString(), LogService.DEFAULT_FORMAT).createSnapshot()) {
            assert snapshot.getError() instanceof DirectoryNotVisibleException;
        }

        try (Snapshot snapshot = getLogService().openLog(aLog.toString(), LogService.DEFAULT_FORMAT).createSnapshot()) {
            assertNull(snapshot.getError());
        }
    }


}
