package com.logviewer.data2;

import com.logviewer.data2.config.ConfigDirHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileFavoriteLogService implements FavoriteLogService, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(FileFavoriteLogService.class);

    private static final String KNOWN_LOGS_FILE = "favorites-logs.txt";

    private final List<String> favorites = new LinkedList<>();

    private final Path favoritesFile;

    public FileFavoriteLogService(ConfigDirHolder environment) {
        favoritesFile = environment.getConfigDir().resolve(KNOWN_LOGS_FILE);
    }

    @Override
    public void afterPropertiesSet() {
        favorites.clear();

        if (Files.isRegularFile(favoritesFile)) {
            try {
                for (String s : Files.readAllLines(favoritesFile, StandardCharsets.UTF_8)) {
                    s = s.trim();
                    if (s.isEmpty() || s.startsWith("#"))
                        continue;

                    favorites.add(s);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<String> getFavorites() {
        synchronized (favorites) {
            return new ArrayList<>(favorites);
        }
    }

    @Override
    public List<String> addFavoriteLog(String path) {
        synchronized (favorites) {
            if (!favorites.contains(path))
                favorites.add(path);

            saveFavorites();

            return new ArrayList<>(favorites);
        }
    }

    @Override
    public List<String> removeFavorite(String path) {
        synchronized (favorites) {
            if (favorites.remove(path)) {
                saveFavorites();
            }

            return new ArrayList<>(favorites);
        }
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    private void saveFavorites() {
        assert Thread.holdsLock(favorites);

        if (Files.isRegularFile(favoritesFile) || !Files.exists(favoritesFile)) {
            try {
                Files.write(favoritesFile, favorites);
            } catch (IOException e) {
                LOG.error("Failed to save known logs list", e);
            }
        }
    }

}
