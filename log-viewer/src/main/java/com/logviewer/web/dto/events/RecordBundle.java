package com.logviewer.web.dto.events;

import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.session.tasks.LoadNextResponse;

import java.util.List;

public class RecordBundle {

    public final List<RestRecord> records;

    public final boolean hasNextLine;

    public RecordBundle(LoadNextResponse data) {
        records = RestRecord.fromPairList(data.getData());
        hasNextLine = data.hasNextLine();
    }
}
