package com.logviewer.data2;

import com.logviewer.data2.net.server.api.RemoteContext;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Triple;

import java.io.Serializable;
import java.util.function.Function;

public class GetFormatAndIdTask implements Function<RemoteContext, Triple<String, String, String>>, Serializable {

    private final String path;

    public GetFormatAndIdTask(String path) {
        this.path = path;
    }

    @Override
    public Triple<String, String, String> apply(RemoteContext ctx) {
        Log log = ctx.getLogService().openLog(path);
        return Triple.create(LvGsonUtils.GSON.toJson(log.getFormat(), LogFormat.class), log.getId(), log.getHostname());
    }
}
