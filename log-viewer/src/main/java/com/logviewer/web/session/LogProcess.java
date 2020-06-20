package com.logviewer.web.session;

public interface LogProcess {

    void setTimeLimit(long timeLimit);

    void start();

    void cancel();

    static long makeTimeLimitNonStrict(boolean backward, long timeLimit) {
        return backward ? timeLimit - 1 : timeLimit + 1;
    }
}
