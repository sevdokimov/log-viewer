package com.logviewer.web;

import com.logviewer.api.LvFileNavigationManager;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.files.FileType;
import com.logviewer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class LogNavigatorController extends AbstractRestRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LogNavigatorController.class);

    private static final String DIR_ICON = "assets/dir.png";

    public static final String CFG_FS_NAVIGATION_ENABLED = "log-viewer.fs-navigation.enabled";

    @Autowired
    private FavoriteLogService favoriteLogService;
    @Autowired
    private LvFileNavigationManager fileManager;
    @Autowired
    private Environment environment;

    @Endpoint
    public RestInitState initState() {
        RestInitState res = new RestInitState();

        res.favoritesEditable = favoriteLogService.isEditable();
        res.favorites = getRestFavorites(favoriteLogService.getFavorites());

        res.showFileTree = isFileTreeAllowed();
        if (res.showFileTree) {
            Path initPath = fileManager.getDefaultDirectory();
            res.initPath = initPath == null ? null : initPath.toString();
            res.initDirContent = getDirContent(initPath);
        }

        return res;
    }

    private boolean isFileTreeAllowed() {
        return environment.getProperty(CFG_FS_NAVIGATION_ENABLED, Boolean.class, true);
    }

    private List<FsItem> getDirContent(@Nullable Path dir) {
        if (!isFileTreeAllowed())
            throw new RestException(403, "File system navigation is disabled");

        List<LvFileNavigationManager.LvFsItem> items = fileManager.getChildren(dir);
        if (items == null)
            return null;

        return createFileItems(items);
    }

    private List<FsItem> createFileItems(List<LvFileNavigationManager.LvFsItem> files) {
        List<FsItem> res = new ArrayList<>(files.size());
        for (LvFileNavigationManager.LvFsItem file : files) {
            if (file.isDirectory()) {
                res.add(new DirItem(file.getPath(), DIR_ICON));
            } else {
                res.add(new FileItem(file.getPath(), file.getType(), file.getSize(), file.getModificationTime()));
            }
        }

        List<String> favorites = favoriteLogService.getFavorites();
        for (FsItem item : res) {
            if (favorites.contains(item.path))
                item.attr.put("favorite", true);
        }

        res.sort(Comparator.naturalOrder());

        return res;
    }

    @Endpoint
    public List<?> listDir() {
        String dir = getRequest().getParameter("dir");

        List<LvFileNavigationManager.LvFsItem> files = fileManager.getChildren(Paths.get(dir));
        if (files == null)
            throw new RestException(401, "Failed to read " + dir);

        return createFileItems(files);
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public List<RestFileState> addFavoriteLog(String path) throws IOException {
        List<String> favorites = favoriteLogService.addFavoriteLog(path);
        return getRestFavorites(favorites);
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public List<RestFileState> removeFavoriteLog(String path) throws IOException {
        List<String> favorites = favoriteLogService.removeFavorite(path);
        return getRestFavorites(favorites);
    }

    private List<RestFileState> getRestFavorites(List<String> src) {
        List<RestFileState> res = new ArrayList<>(src.size());

        for (String logPath : src) {
            RestFileState log = new RestFileState();
            log.path = logPath;

            Path path = Paths.get(logPath);
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                if (attrs.isRegularFile()) {
                    log.lastModification = attrs.lastModifiedTime().toMillis();
                    log.size = attrs.size();
                }
            } catch (IOException ignored) {

            }

            res.add(log);
        }

        return res;
    }

    public static class RestInitState {
        private List<RestFileState> favorites;

        private boolean favoritesEditable;

        private boolean showFileTree;

        private String initPath;

        private List<FsItem> initDirContent;
    }

    public static class RestFileState {
        private String path;
        private Long size;
        private Long lastModification;
    }

    private static abstract class FsItem implements Comparable<FsItem> {
        protected final String path;
        protected final String name;
        protected final String icon;
        protected final boolean isDirectory;

        protected final Map<String, Object> attr = new HashMap<>();

        FsItem(Path path, String icon, boolean isDirectory) {
            this.path = path.toString();
            
            if (path.toString().equals("/")) {
                name = "/";
            } else {
                Path fileName = path.getFileName();
                this.name = fileName == null ? "" : fileName.toString();
            }
            
            this.icon = icon;
            this.isDirectory = isDirectory;
        }

        @Override
        public int compareTo(@Nonnull LogNavigatorController.FsItem o) {
            if (isDirectory != o.isDirectory)
                return isDirectory ? -1 : 1;

            return Utils.compareFileNames(name, o.name);
        }
    }

    private static class DirItem extends FsItem {
        private List<?> singleItem;

        public DirItem(Path path, String icon) {
            super(path, icon, true);
        }
    }

    private static class FileItem extends FsItem {
        private final String type;
        private final long size;
        private final Long modificationTime;

        public FileItem(Path path, FileType type, long size, @Nullable Long modificationTime) {
            super(path, type.getIcon(), false);
            this.type = type.getTypeId();
            this.size = size;
            this.modificationTime = modificationTime;
        }
    }

}
