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

    public static final String CFG_SHOW_FILE_TREE = "log-viewer.show-file-tree";

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

        if (environment.getProperty(CFG_SHOW_FILE_TREE, Boolean.class, true))
            res.treeRoot = loadTreeRoot();

        return res;
    }

    private DirItem loadTreeRoot() {
        List<DirItem> items = new ArrayList<>();

        DirItem userRoot = createTreeForDirectory(fileManager.getDefaultDirectory());

        List<LvFileNavigationManager.LvFsItem> roots = fileManager.getChildren(null);
        if (roots == null) {
            items.add(userRoot);
        } else {
            for (LvFileNavigationManager.LvFsItem root : roots) {
                if (root.getPath().toString().equals(userRoot.path)) {
                    items.add(userRoot);
                }
                else {
                    items.add(new DirItem(root.getPath(), DIR_ICON, null));
                }
            }
        }

        return new DirItem(Paths.get(""), "", items);
    }

    private DirItem createTreeForDirectory(Path dir) {
        List<Path> path = new ArrayList<>();

        for (Path f = dir; f != null; f = f.getParent()) {
            path.add(f);
        }

        Collections.reverse(path);

        DirItem root = new DirItem(path.get(0), DIR_ICON, null);

        DirItem f = root;

        int i = 0;
        do {
            List<LvFileNavigationManager.LvFsItem> files = fileManager.getChildren(path.get(i));
            if (files != null) {
                f.items = createFileItems(files);
            }

            if (++i >= path.size())
                break;

            String dirName = path.get(i).getFileName().toString();

            if (f.items == null) {
                DirItem dirItem = new DirItem(path.get(i), DIR_ICON, null);
                f.items = Collections.singletonList(dirItem);
                f = dirItem;
            } else {
                f = (DirItem) ((List<FsItem>)f.items).stream().filter(item -> item.name.equals(dirName)).findFirst().orElse(null);
            }
        }  while (f != null);

        return root;
    }

    private List<FsItem> createFileItems(List<LvFileNavigationManager.LvFsItem> files) {
        List<FsItem> res = new ArrayList<>(files.size());
        for (LvFileNavigationManager.LvFsItem file : files) {
            if (file.isDirectory()) {
                res.add(new DirItem(file.getPath(), DIR_ICON, null));
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

        private DirItem treeRoot;
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
        private List<?> items;

        public DirItem(Path path, String icon, List<?> items) {
            super(path, icon, true);
            this.items = items;
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
