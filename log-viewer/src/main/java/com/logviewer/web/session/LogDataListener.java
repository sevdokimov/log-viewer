package com.logviewer.web.session;

import com.logviewer.data2.RecordList;
import org.springframework.lang.NonNull;

public interface LogDataListener {
    void onData(@NonNull RecordList data);

    void onFinish(@NonNull Status status, boolean eof);
}
