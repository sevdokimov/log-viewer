package com.logviewer.data2.net.server.api;

import com.logviewer.data2.Position;
import com.logviewer.utils.Triple;
import com.logviewer.web.session.tasks.SearchPattern;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface RemoteApi {

    CompletableFuture<Triple<String, String, String>> getFormatAndId(String path);

    RecordLoaderChannel createRecordLoaderChannel(String file, String format, Position start, boolean backward, String hash,
                                                  String filter, int recordCountLimit, long sizeLimit, Consumer listener);

    RecordLoaderChannel createRecordSearcherChannel(String file, String format, Position start, boolean backward, String hash,
                                                    String filter, int recordCountLimit, SearchPattern searchPattern, Consumer listener);
}
