package com.logviewer.data2;

import java.util.List;

public interface FavoriteLogService {

    List<String> getFavorites();

    List<String> addFavoriteLog(String path);

    List<String> removeFavorite(String path);

    boolean isEditable();
}
