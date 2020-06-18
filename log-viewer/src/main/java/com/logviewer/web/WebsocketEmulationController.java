package com.logviewer.web;

import com.google.common.base.Throwables;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.LvTimer;
import com.logviewer.web.dto.events.BackendErrorEvent;
import com.logviewer.web.dto.events.BackendEvent;
import com.logviewer.web.rmt.MethodCall;
import com.logviewer.web.rmt.RemoteInvoker;
import com.logviewer.web.session.LogSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebsocketEmulationController extends AbstractRestRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketEmulationController.class);

    @Value("${log-viewer.ws-emulator.max-connection-count:1000}")
    private int maxConnections;

    @Value("${log-viewer.ws-emulator.max-message-queue-size:40}")
    private int maxMessageQueueSize;

    @Value("${log-viewer.ws-emulator.connection-hold-time:20000}")
    private long connectionHoldTime;

    @Value("${log-viewer.ws-emulator.wait-connection-timeout:10000}")
    private long waitConnectionTimeout;

    @Autowired
    private LvTimer timer;
    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ConnectionSession> sessions = new HashMap<>();

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public void closeSession(String sessionId) {
        ConnectionSession session;

        synchronized (sessions) {
            session = sessions.remove(sessionId);
        }

        if (session != null)
            session.close("page closed");
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public Object request(RestRequestBody body) throws Throwable {
        LOG.debug("handling request [sessionId={}, requestNumber={}-{}]", body.sessionId,
                body.messages.length == 0 ? "<none>" : body.messages[0].messageNumber,
                body.messages.length == 0 ? "<none>" : body.messages[body.messages.length - 1].messageNumber);

        ConnectionSession connectionSession;

        synchronized (sessions) {
            connectionSession = sessions.get(body.sessionId);
            if (connectionSession == null) {
                if (body.messages.length == 0 || body.messages[0].messageNumber != 0)
                    throw new RestException(410, "Server connection has been closed");

                if (sessions.size() >= maxConnections)
                    throw new RestException(429, "Too many connections: " + maxConnections);

                connectionSession = new ConnectionSession(body.sessionId, getUserName());

                sessions.put(body.sessionId, connectionSession);

                LOG.info("Connection opened [sessionId={}, user={}]", body.sessionId, connectionSession.userName);
            }
        }

        return connectionSession.handleRequest(getRequest(), body.messages);
    }

    private String getUserName() {
        Principal userPrincipal = getRequest().getUserPrincipal();
        if (userPrincipal == null)
            return "<anonymous>";

        return userPrincipal.getName();
    }

    @Override
    public void close() {
        Collection<ConnectionSession> connectionSessions;

        synchronized (sessions) {
            connectionSessions = new ArrayList<>(sessions.values());
            sessions.clear();
        }

        for (ConnectionSession value : connectionSessions) {
            try {
                value.close("application stopping");
            } catch (Throwable e) {
                LOG.error("Failed to close page: {}", value.sessionId, e);
            }
        }
    }

    private class ConnectionSession {

        private final String sessionId;

        private final LogSession logSession;

        private long backendMessageCounter;
        private final Queue<ToBackendMessage> toBackendQueue = new PriorityQueue<>();

        private long uiMessageCounter;
        private final List<ToUiMessage> toUiQueue = new ArrayList<>();

        private AsyncContext asyncContext;
        private TimerTask asyncContextChecker;

        private final AtomicBoolean closed = new AtomicBoolean();

        private final String userName;

        public ConnectionSession(@Nonnull String sessionId, @Nonnull String userName) {
            this.sessionId = sessionId;
            this.userName = userName;

            logSession = LogSession.fromContext(this::sendEvent, applicationContext);
        }

        Object handleRequest(HttpServletRequest request, ToBackendMessage[] requests) throws Throwable {
            try {
                addRequestToQueue(requests);

                processRequestsInQueue();

                synchronized (toUiQueue) {
                    if (closed.get())
                        return Collections.emptyList();

                    if (asyncContextChecker != null)
                        asyncContextChecker.cancel();

                    if (asyncContext != null) {
                        LOG.debug("release held connection [sessionId={}, sentMessages={}]", sessionId, toUiQueue.size());

                        writeResponse(asyncContext.getResponse(), toUiQueue);
                        toUiQueue.clear();
                        asyncContext.complete();
                        asyncContext = null;
                    }

                    if (!toUiQueue.isEmpty()) {
                        ArrayList<ToUiMessage> res = new ArrayList<>(toUiQueue);
                        toUiQueue.clear();

                        asyncContextChecker = new TimeoutChecker(); // Close logSession is UI doesn't connect long time.
                        timer.schedule(asyncContextChecker, waitConnectionTimeout);

                        LOG.debug("return response [sessionId={}, sentMessages={}", sessionId, res.size());

                        return res;
                    } else {
                        asyncContext = request.startAsync();
                        asyncContext.setTimeout(connectionHoldTime + 10_000);

                        asyncContextChecker = new AsyncContextCloser(asyncContext);
                        timer.schedule(asyncContextChecker, connectionHoldTime);

                        LOG.debug("connection has held [sessionId={}]", sessionId);

                        return asyncContext;
                    }
                }
            } catch (Throwable e){
                close("internal error");
                throw e;
            }
        }

        private void addRequestToQueue(ToBackendMessage[] messages) {
            synchronized (toBackendQueue) {
                if (toUiQueue.size() >= maxMessageQueueSize)
                    throw new RestException(429, "Too many connections: " + sessionId);

                Collections.addAll(toBackendQueue, messages);
            }
        }

        private void sendEvent(BackendEvent event) {
            synchronized (toUiQueue) {
                if (closed.get())
                    return;

                toUiQueue.add(new ToUiMessage(uiMessageCounter++, event));

                if (asyncContext != null)
                    sendResponseQueueToAsync();
            }
        }

        private void processRequestsInQueue() {
            while (true) {
                ToBackendMessage r;

                synchronized (toBackendQueue) {
                    r = toBackendQueue.peek();
                    if (r == null || r.messageNumber != backendMessageCounter)
                        return;

                    toBackendQueue.remove();
                }

                try {
                    RemoteInvoker.call(logSession, r.event);
                } catch (Throwable e) {
                    if (e instanceof InvocationTargetException) {
                        // an expected exception, the exception is mentioned in "throws" list in method declaration.
                        e = ((InvocationTargetException)e).getTargetException();
                    }

                    LOG.error("Remote method execution error", e);

                    sendEvent(new BackendErrorEvent(Throwables.getStackTraceAsString(e)));
                    break;
                }

                synchronized (toBackendQueue) {
                    backendMessageCounter++;
                }
            }

        }

        private void close(@Nonnull String reason) {
            assert !Thread.holdsLock(toBackendQueue);
            assert !Thread.holdsLock(toUiQueue);

            if (closed.compareAndSet(false, true)) {
                synchronized (sessions) {
                    sessions.remove(sessionId, this);
                }

                logSession.shutdown();

                synchronized (toUiQueue) {
                    if (asyncContextChecker != null)
                        asyncContextChecker.cancel();

                    if (asyncContext != null) {
                        asyncContext.complete();
                        asyncContext = null;
                    }
                }

                LOG.info("Connection closed [sessionId={}, user={}, reason={}]", sessionId, userName, reason);
            }
        }

        private void writeResponse(ServletResponse resp, @Nullable List<ToUiMessage> res) throws IOException {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            if (res == null) {
                resp.getWriter().write("[]");
            } else {
                LvGsonUtils.GSON.toJson(res, resp.getWriter());
            }
        }

        private void sendResponseQueueToAsync() {
            assert Thread.holdsLock(toUiQueue);

            assert asyncContextChecker instanceof AsyncContextCloser;
            asyncContextChecker.cancel();

            boolean success = false;

            try {
                LOG.debug("release held connection [sessionId={}, sentMessages={}]", sessionId, toUiQueue.size());
                
                writeResponse(asyncContext.getResponse(), toUiQueue);
                toUiQueue.clear();
                asyncContext.complete();
                asyncContext = null;

                asyncContextChecker = new TimeoutChecker(); // Close logSession is UI doesn't connect long time.
                timer.schedule(asyncContextChecker, waitConnectionTimeout);

                success = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (!success)
                    close("internal error");
            }
        }



        private class AsyncContextCloser extends TimerTask {

            private final WeakReference<AsyncContext> expectedContext;

            public AsyncContextCloser(AsyncContext asyncContext) {
                expectedContext = new WeakReference<>(asyncContext);
            }

            @Override
            public void run() {
                synchronized (toUiQueue) {
                    if (closed.get())
                        return;

                    AsyncContext context = expectedContext.get();
                    if (context == null || context != asyncContext)
                        return;

                    sendResponseQueueToAsync();
                }
            }
        }

        private class TimeoutChecker extends TimerTask {
            @Override
            public void run() {
                close("timeout");
            }
        }
    }

    private static class ToUiMessage {
        private final long messageNumber;

        private final BackendEvent event;

        public ToUiMessage(long responseNumber, BackendEvent event) {
            this.messageNumber = responseNumber;
            this.event = event;
        }
    }

    private static class ToBackendMessage implements Comparable<ToBackendMessage> {
        private long messageNumber;

        private MethodCall event;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ToBackendMessage)) return false;
            ToBackendMessage request = (ToBackendMessage) o;
            return messageNumber == request.messageNumber;
        }

        @Override
        public int hashCode() {
            return (int)messageNumber;
        }

        @Override
        public int compareTo(ToBackendMessage o) {
            return Long.compare(messageNumber, o.messageNumber);
        }
    }

    private static class RestRequestBody {
        private String sessionId;

        private ToBackendMessage[] messages;
    }
}
