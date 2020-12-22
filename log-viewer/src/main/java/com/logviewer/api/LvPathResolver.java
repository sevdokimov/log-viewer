package com.logviewer.api;

import com.logviewer.data2.LogPath;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;

public interface LvPathResolver {

    @Nullable
    Collection<LogPath> resolvePath(@NonNull String pathFromHttpParameter);

}
