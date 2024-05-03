package com.logviewer.web;

import com.logviewer.data2.LogContextHolder;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.RuntimeInterruptedException;
import com.logviewer.utils.Utils;
import com.logviewer.web.dto.events.BackendErrorEvent;
import com.logviewer.web.dto.events.BackendEvent;
import com.logviewer.web.rmt.MethodCall;
import com.logviewer.web.rmt.RemoteInvoker;
import com.logviewer.web.session.LogSession;
import com.logviewer.web.session.SessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.websocket.Endpoint;
import javax.websocket.*;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;

import static javax.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static javax.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;

public class LogViewerWebsocket extends Endpoint {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewerWebsocket.class);

    private static final String LOG_SESSION = "log-session";

    @Autowired
    private ApplicationContext applicationContext;
    
    @Override
    public void onOpen(Session webSession, EndpointConfig config) {
        LOG.info("Connection opened: {} {}", webSession.getId(), getUserName(webSession));

        webSession.setMaxIdleTimeout(0);

        ApplicationContext context = LogContextHolder.getInstance();
        if (context == null) {
            context = applicationContext;
            
            if (context == null) {
                throw new RuntimeException("Spring context not found. Set ApplicationContext to " +
                        "com.logviewer.data2.LogContextHolder.setInstance(appCtx)");
            }
        }

        SessionAdapter sessionAdapter = event -> sendMessage(webSession, event);

        LogSession session = LogSession.fromContext(sessionAdapter, applicationContext);

        webSession.getUserProperties().put(LOG_SESSION, session);

        //noinspection Convert2Lambda
        webSession.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    MethodCall call = LvGsonUtils.GSON.fromJson(message, MethodCall.class);

                    RemoteInvoker.call(session, call);
                } catch (Throwable e) {
                    if (e instanceof InvocationTargetException) {
                        // an expected exception, the exception is mentioned in "throws" list in method declaration.
                        e = ((InvocationTargetException) e).getTargetException();
                    }

                    LOG.error("Remote method execution error", e);

                    sessionAdapter.send(new BackendErrorEvent(Utils.getStackTraceAsString(e)));
                }
            }
        });
    }

    private void sendMessage(Session webSession, BackendEvent event) {
        String json = LvGsonUtils.GSON.toJson(event);

        synchronized (webSession) {
            if (Thread.currentThread().isInterrupted())
                throw new RuntimeInterruptedException();
            if (LOG.isDebugEnabled())
                LOG.debug("Send message " + json);

            webSession.getAsyncRemote().sendText(json, result -> {
                if (!result.isOK() && webSession.isOpen()) {
                    LOG.error("Failed to send message", result.getException());
                    Utils.closeQuietly(webSession);
                }
            });
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        LOG.error("websocket error", thr);
        close(session);
    }

    private void close(Session session) {
        LogSession logSession = (LogSession) session.getUserProperties().get(LOG_SESSION);
        if (logSession != null)
            logSession.shutdown();
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
}
