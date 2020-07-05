package com.logviewer.web.session;

import com.logviewer.data2.RecordList;

public interface LogDataListener {
    void onData(RecordList data);

    void onFinish(Status status, boolean eof);
}
