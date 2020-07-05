package com.logviewer.impl;

import com.logviewer.data2.FavoriteLogService;

import java.util.ArrayList;
import java.util.List;

public class InmemoryFavoritesService implements FavoriteLogService {

    private final List<String> favorites = new ArrayList<>();

    private boolean editable = true;

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void clear() {
        favorites.clear();
    }

    @Override
    public synchronized List<String> getFavorites() {
        return new ArrayList<>(favorites);
    }

    @Override
    public synchronized List<String> addFavoriteLog(String path) {
        if (!isEditable())
            throw new IllegalStateException("Favorites list is not editable");

        if (!favorites.contains(path))
            favorites.add(path);

        return new ArrayList<>(favorites);
    }

    @Override
    public List<String> removeFavorite(String path) {
        if (!isEditable())
            throw new IllegalStateException("Favorites list is not editable");

        favorites.remove(path);
        return new ArrayList<>(favorites);
    }

    @Override
    public boolean isEditable() {
        return editable;
    }
}
