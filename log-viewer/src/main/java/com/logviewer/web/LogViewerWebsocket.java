package com.logviewer.web;

import com.google.common.base.Throwables;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.web.dto.events.BackendErrorEvent;
import com.logviewer.web.rmt.MethodCall;
import com.logviewer.web.rmt.RemoteInvoker;
import com.logviewer.web.session.LogSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.websocket.Endpoint;
import javax.websocket.*;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;

import static javax.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static javax.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;

public class LogViewerWebsocket extends Endpoint {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewerWebsocket.class);

    @Override
    public void onOpen(Session webSession, EndpointConfig config) {
        LOG.info("Connection opened: {} {}", webSession.getId(), getUserName(webSession));

        webSession.setMaxIdleTimeout(0);

        webSession.addMessageHandler(new LogWebSocketHandler(webSession));
    }

    @Override
    public void onError(Session session, Throwable thr) {
        LOG.error("websocket error", thr);
        close(session);
    }

    private void close(Session session) {
        for (MessageHandler handler : session.getMessageHandlers()) {
            if (handler instanceof LogWebSocketHandler) {
                ((LogWebSocketHandler) handler).close();
            }
        }
    }

    @Override
    public void onClose(Session webSession, CloseReason reason) {
        String text = "Connection closed (" + reason.toString() + ") " + webSession.getId() + " " + getUserName(webSession);
        if (reason.getCloseCode() == NORMAL_CLOSURE || reason.getCloseCode() == GOING_AWAY) {
            LOG.info(text);
        }
        else {
            LOG.error(text);
        }
        
        close(webSession);
    }

    private static String getUserName(Session webSession) {
        Principal userPrincipal = webSession.getUserPrincipal();
        if (userPrincipal == null)
            return "<anonymous>";

        return userPrincipal.getName();
    }

    private static class LogWebSocketHandler implements MessageHandler.Whole<String> {
        private final LogSession session;

        private WebsocketSessionAdapter sessionAdapter;

        LogWebSocketHandler(Session webSession) {
            sessionAdapter = new WebsocketSessionAdapter(webSession);

            ApplicationContext context = LogContextHolder.getInstance();
            session = LogSession.fromContext(sessionAdapter, context);
        }

        public void close() {
            session.shutdown();
        }

        @Override
        public void onMessage(String message) {
            try {
                MethodCall call = LvGsonUtils.GSON.fromJson(message, MethodCall.class);

                RemoteInvoker.call(session, call);
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    // an expected exception, the exception is mentioned in "throws" list in method declaration.
                    e = ((InvocationTargetException)e).getTargetException();
                }

                LOG.error("Remote method execution error", e);

                sessionAdapter.send(new BackendErrorEvent(Throwables.getStackTraceAsString(e)));
            }
        }

    }
}
