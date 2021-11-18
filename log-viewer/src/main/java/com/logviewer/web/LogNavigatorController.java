package com.logviewer.web;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.api.LvFileNavigationManager;
import com.logviewer.data2.FavoriteLogService;
import com.logviewer.files.FileType;
import com.logviewer.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class LogNavigatorController extends AbstractRestRequestHandler {

    private static final String DIR_ICON = "assets/dir.png";

    public static final String CFG_FS_NAVIGATION_ENABLED = "log-viewer.fs-navigation.enabled";

    @Autowired
    private FavoriteLogService favoriteLogService;
    @Autowired
    private LvFileNavigationManager fileManager;
    @Autowired
    private LvFileAccessManager fileAccessManager;
    @Autowired
    private Environment environment;

    @Endpoint
    public RestInitState initState() {
        RestInitState res = new RestInitState();

        res.favoritesEditable = favoriteLogService.isEditable();
        res.favorites = getRestFavorites(favoriteLogService.getFavorites());

        res.showFileTree = isFileTreeAllowed();
        if (res.showFileTree) {
            Path defaultDirectoryFromConfig = fileManager.getDefaultDirectory();
            res.defaultDir = defaultDirectoryFromConfig == null ? null : defaultDirectoryFromConfig.toString();

            String initialDir = getRequest().getParameter("initialDir");

            Path initDir;

            if (StringUtils.isEmpty(initialDir)) {
                initDir = defaultDirectoryFromConfig;
            } else {
                initDir = Paths.get(initialDir);
            }
            
            res.initDir = initDir == null ? null : initDir.toString();
            res.initDirContent = getDirContent(initDir);
        }

        return res;
    }

    private boolean isFileTreeAllowed() {
        return environment.getProperty(CFG_FS_NAVIGATION_ENABLED, Boolean.class, true);
    }

    /**
     * @return {error: string, content: FsItem[]}
     */
    private RestContent getDirContent(@Nullable Path dir) {
        if (!isFileTreeAllowed())
            return new RestContent("File system navigation is disabled");

        try {
            List<LvFileNavigationManager.LvFsItem> items = fileManager.getChildren(dir);
            return new RestContent(createFileItems(items));
        } catch (SecurityException e) {
            return new RestContent(e.getMessage());
        } catch (IOException e) {
            return new RestContent("Failed to load file list: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
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
    public RestContent listDir() {
        String dir = getRequest().getParameter("dir");

        return getDirContent(Paths.get(dir));
    }

    @Endpoint
    public RestOpenPathResponse openCustomDir() {
        String dir = getRequest().getParameter("dir");

        if (!isFileTreeAllowed())
            return new RestOpenPathResponse("File system navigation is disabled");

        Path path = Paths.get(dir);

        if (!path.isAbsolute())
            return new RestOpenPathResponse("Path is not absolute");

        if (Files.isDirectory(path))
            return new RestOpenPathResponse(getDirContent(path), null, dir);

        if (Files.isRegularFile(path)) {
            if (!fileAccessManager.isFileVisible(path))
                return new RestOpenPathResponse(fileAccessManager.errorMessage(path));

            return new RestOpenPathResponse(getDirContent(path.getParent()), path.toString(), path.getParent().toString());
        }

        if (!fileAccessManager.isDirectoryVisible(path)) {
            return new RestOpenPathResponse(fileAccessManager.errorMessage(path));
        }

        return new RestOpenPathResponse("Directory not found");
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public List<RestFileState> addFavoriteLog(String path) {
        List<String> favorites = favoriteLogService.addFavoriteLog(path);
        return getRestFavorites(favorites);
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public List<RestFileState> removeFavoriteLog(String path) {
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

        private String initDir; // Initial directory for the file navigation dialog. May be "defaultPath" or a value from URL

        private String defaultDir; // Default derictory defined in the configuration

        // {error: string, content: FsItem[]}
        private RestContent initDirContent;
    }

    public static class RestFileState {
        private String path;
        private Long size;
        private Long lastModification;
    }

    private static class RestContent {
        private String error;
        private List<FsItem> content;

        public RestContent(String error) {
            this.error = error;
        }

        public RestContent(List<FsItem> content) {
            this.content = content;
        }
    }

    private static class RestOpenPathResponse {
        private RestContent content;
        private String selectedPath;
        private String newCurrentDir;

        public RestOpenPathResponse(String error) {
            this(new RestContent(error), null, null);
        }

        public RestOpenPathResponse(RestContent content, String selectedPath, String newCurrentDir) {
            this.content = content;
            this.selectedPath = selectedPath;
            this.newCurrentDir = newCurrentDir;
        }
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
        public int compareTo(@NonNull LogNavigatorController.FsItem o) {
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
