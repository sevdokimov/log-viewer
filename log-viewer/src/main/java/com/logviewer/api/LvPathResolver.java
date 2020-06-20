package com.logviewer.api;

import com.logviewer.data2.LogPath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface LvPathResolver {

    @Nullable
    Collection<LogPath> resolvePath(@Nonnull String pathFromHttpParameter);

}
