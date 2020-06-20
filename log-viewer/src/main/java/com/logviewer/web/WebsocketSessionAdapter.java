package com.logviewer.web;

import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.Utils;
import com.logviewer.web.dto.events.BackendEvent;
import com.logviewer.web.session.SessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.websocket.Session;

public class WebsocketSessionAdapter implements SessionAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketSessionAdapter.class);

    private final Session webSession;

    public WebsocketSessionAdapter(Session webSession) {
        this.webSession = webSession;
    }

    @Override
    public void send(@Nonnull BackendEvent event) {
        send(LvGsonUtils.GSON.toJson(event));
    }

    private synchronized void send(@Nonnull String jsonObject) {
        if (Thread.currentThread().isInterrupted())
            throw new RuntimeInterruptedException();

        if (LOG.isDebugEnabled())
            LOG.debug("Send message " + jsonObject);

        webSession.getAsyncRemote().sendText(jsonObject, result -> {
            if (!result.isOK() && webSession.isOpen()) {
                LOG.error("Failed to send message", result.getException());
                Utils.closeQuietly(webSession);
            }
        });
    }
}
