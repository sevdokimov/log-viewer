package com.logviewer.net;

import com.logviewer.LogSessionTestBase;
import com.logviewer.data2.*;
import com.logviewer.data2.net.Node;
import com.logviewer.data2.net.OutcomeConnection;
import com.logviewer.data2.net.RemoteNodeService;
import com.logviewer.data2.net.server.LogViewerBackdoorServer;
import com.logviewer.data2.net.server.api.RemoteTask;
import com.logviewer.data2.net.server.api.RemoteTaskContext;
import com.logviewer.data2.net.server.api.RemoteTaskController;
import com.logviewer.utils.LvGsonUtils;
import com.logviewer.utils.Triple;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class ConnectionTest extends LogSessionTestBase {

    @Test
    public void connectionError() throws InterruptedException {
        ApplicationContext ctx = getCommonContext();

        CompletableFuture<OutcomeConnection> localhost = ctx.getBean(RemoteNodeService.class).getNodeConnection(new Node("localhost", 19823));
        try {
            localhost.get();
            assert false;
        } catch (ExecutionException e) {
            assert e.getCause() instanceof IOException;
            assert e.getCause().getMessage().contains("Connection refused");
        }
    }

    @Test
    public void testApiCall() throws Exception {
        doRemoteTest((local, remote) -> {
            String logFile = getTestLog("Predicate.log");

            CompletableFuture<OutcomeConnection> nodeConnection = local.getRemoteNodeService().getNodeConnection(NODE);
            OutcomeConnection connection = nodeConnection.get();

            Triple<String, String, String> formatAndId = connection.execute(new GetFormatAndIdTask(logFile)).get();

            Log log = remote.openLog(logFile);

            assertEquals(log.getId(), formatAndId.getSecond());
            assertEquals(LvGsonUtils.GSON.toJson(log.getFormat(), LogFormat.class), formatAndId.getFirst());
        });
    }

    @Test
    public void tailLoader() throws Exception {
        doRemoteTest((local, remote) -> {
            String logFile = getTestLog("Predicate.log");

            CompletableFuture<LogView> future = local.openRemoteLog(new LogPath(NODE, logFile));
            future.get();
        });
    }

    private void taskTest(RemoteNodeServiceTest remoteNodeServiceTest) throws Exception {
        doRemoteTest((local, remote) -> {
            LogViewerBackdoorServer logServer = findContext(remote).getBean(LogViewerBackdoorServer.class);
            remoteNodeServiceTest.doTest(local.getRemoteNodeService(), logServer);
        });
    }

    interface RemoteNodeServiceTest {
        void doTest(RemoteNodeService remoteNodeService, LogViewerBackdoorServer logServer) throws Exception;
    }

    @Test(timeout = 3000)
    public void generalTask() throws Exception {
        taskTest((remoteNodeService, logServer) -> {
            TestRemoteTask.taskEvents = null;
            TestRemoteTask.state = 0;

            BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

            RemoteTaskController<TestRemoteTask> controller = remoteNodeService.createTask(NODE, new TestRemoteTask(), (s, e) -> {
                responseQueue.add(e == null ? s : e);
            });

            controller.alterTask((Consumer<TestRemoteTask> & Serializable) task -> task.setState(1));

            assert responseQueue.poll(60, TimeUnit.MILLISECONDS) == null; // check task has not started
            assert TestRemoteTask.taskEvents == null; // not started
            
            remoteNodeService.startTask(controller);

            assert "started".equals(responseQueue.take());
            assert TestRemoteTask.state == 1;

            controller.alterTask((Consumer<TestRemoteTask> & Serializable) task -> task.setState(2));
            assert "state_2".equals(responseQueue.take());
            assert TestRemoteTask.state == 2;

            TestRemoteTask.taskEvents.add("s");
            assertEquals("s_2", responseQueue.take());

            TestRemoteTask.taskEvents.add("finish"); // close task

            assert "finish".equals(responseQueue.take());

            // Send message to closed task
            TestRemoteTask.taskEvents.add("eventAfterFinish");

            assert responseQueue.poll(60, TimeUnit.MILLISECONDS) == null; // No response, task was closed after "finish" message

            // check no error when send message to closed task
            controller.alterTask((Consumer<TestRemoteTask> & Serializable) task -> task.setState(3));
        });
    }

    @Test(timeout = 3000)
    public void taskFinishedWithError() throws Exception {
        taskTest((remoteNodeService, logServer) -> {
            TestRemoteTask.taskEvents = null;
            TestRemoteTask.state = 0;

            BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

            RemoteTaskController<TestRemoteTask> controller = remoteNodeService.startTask(NODE, new TestRemoteTask(), (s, e) -> {
                responseQueue.add(e == null ? s : e);
            });

            assert "started".equals(responseQueue.take());
            TestRemoteTask.taskEvents.add("s");

            assertEquals("s_0", responseQueue.take());

            TestRemoteTask.taskEvents.add(new IllegalStateException()); // finish task with exception

            assert responseQueue.take() instanceof IllegalStateException;

            // Send message to closed task
            TestRemoteTask.taskEvents.add("eventAfterFinish");

            assert responseQueue.poll(60, TimeUnit.MILLISECONDS) == null; // No response

            // check no error when send message to closed task
            controller.alterTask((Consumer<TestRemoteTask> & Serializable) task -> task.setState(3));
        });
    }

    @Test(timeout = 3000)
    public void cancelTask() throws Exception {
        taskTest((remoteNodeService, logServer) -> {
            TestRemoteTask.taskEvents = null;
            TestRemoteTask.state = 0;

            BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

            RemoteTaskController<TestRemoteTask> controller = remoteNodeService.startTask(NODE, new TestRemoteTask(), (s, e) -> {
                responseQueue.add(e == null ? s : e);
            });

            assert "started".equals(responseQueue.take());
            TestRemoteTask.taskEvents.add("s");

            assertEquals("s_0", responseQueue.take());

            controller.cancel();

            // Send message to closed task
            TestRemoteTask.taskEvents.add("eventAfterFinish");

            assert responseQueue.poll(60, TimeUnit.MILLISECONDS) == null; // No response

            // check no error when send message to closed task
            controller.alterTask((Consumer<TestRemoteTask> & Serializable) task -> task.setState(3));
            controller.cancel();
        });
    }

    @Test(timeout = 3000)
    public void testDisconnect() throws Exception {
        taskTest((remoteNodeService, logServer) -> {
            TestRemoteTask.taskEvents = null;
            TestRemoteTask.state = 0;

            BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

            RemoteTaskController<TestRemoteTask> controller = remoteNodeService.startTask(NODE, new TestRemoteTask(), (s, e) -> {
                responseQueue.add(e == null ? s : e);
            });

            assert "started".equals(responseQueue.take());
            TestRemoteTask.taskEvents.add("s");

            assertEquals("s_0", responseQueue.take());

            logServer.destroy();

            assert responseQueue.take() instanceof IOException;
            
            // check no error when send message to closed task
            controller.alterTask((Consumer<TestRemoteTask> & Serializable) task -> task.setState(3));
            controller.cancel();
        });
    }

    @Test(timeout = 3000)
    public void taskFailedOnStart() throws Exception {
        taskTest((remoteNodeService, logServer) -> {
            BlockingQueue<Throwable> exceptionHolder = new LinkedBlockingQueue<>();

            RemoteTaskController<BrokenTask> controller = remoteNodeService.startTask(NODE, new BrokenTask(), (s, e) -> {
                exceptionHolder.add(e);
            });

            Throwable error = exceptionHolder.take();
            assertEquals(UnsupportedOperationException.class, error.getClass());
            
            // check no error when send message to closed task
            controller.alterTask((Consumer<BrokenTask> & Serializable) Object::hashCode);
            controller.cancel();
        });
    }

    private static class BrokenTask implements RemoteTask<String> {

        @Override
        public void start(@Nonnull RemoteTaskContext<String> ctx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {

        }
    }

    private static class TestRemoteTask implements RemoteTask<String> {

        static BlockingQueue<Object> taskEvents;

        static int state;

        private Future<?> future;

        @Override
        public void start(@Nonnull RemoteTaskContext<String> ctx) {
            assert future == null;

            assert taskEvents == null;
            taskEvents = new LinkedBlockingQueue<>();

            future = ctx.getLogService().getExecutor().submit(() -> {
                ctx.send("started");
                try {
                    while (true) {
                        Object e = taskEvents.take();
                        if (e instanceof Throwable)
                            ctx.sendErrorAndCloseChannel((Throwable) e);

                        String s = (String) e;
                        if (s.equals("finish")) {
                            ctx.sendAndCloseChannel(s);
                            break;
                        }
                        
                        ctx.send(s + "_" + state);
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        public void assertNotStarted() {
            if (taskEvents != null)
                taskEvents = null; // broke task
        }

        public void setState(int newState) {
            state = newState;

            if (taskEvents != null)
                taskEvents.add("state");
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }
    }
}
