package com.logviewer.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class LogGeneratorService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LogGeneratorService.class);

    private static final int THREAD_COUNT = 6;

    @Value("${server.port:8080}")
    private int logViewerPort;

    private ExecutorService executorService;

    @Override
    public void destroy() throws InterruptedException {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
            executorService = null;
        }
    }

    @Override
    public void afterPropertiesSet() {
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(this::generate);
        }
    }

    public String logViewerUrl() {
        return "Log Viewer URL: http://localhost:" + logViewerPort + "/logs";
    }

    private void generate() {
        Random rnd = new Random();

        try {
            while (true) {
                Thread.sleep(rnd.nextInt(6000));

                int x = rnd.nextInt(11);
                switch (x) {
                    case 0:
                    case 1:
                        log.debug(rnd.ints(10, 1, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
                        break;

                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        log.info(rnd.ints(10, 1, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
                        break;

                    case 6:
                    case 7:
                        log.warn(rnd.ints(10, 1, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
                        break;

                    case 8:
                        log.error(rnd.ints(10, 1, 10000).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
                        break;

                    case 9:
                        try {
                            throwException();
                        } catch (Exception e) {
                            log.error("Failed to execute method [timestamp={}, user=anonymous, time={}ms]",
                                    System.currentTimeMillis(), rnd.nextInt(4*60*1000), e);
                        }
                        break;

                    case 10:
                        log.info(logViewerUrl());
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        } catch (InterruptedException ignored) {

        }
    }

    private void throwException() {
        doSomething();
    }

    private void doSomething() {
        List<String> list = new ArrayList<>();
        list.addAll(null);
    }
}
