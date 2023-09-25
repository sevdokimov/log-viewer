package com.logviewer.formats;

import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogReader;
import org.springframework.lang.NonNull;

public interface FieldSet {

    LogFormat.FieldDescriptor[] getFields();

    @NonNull
    LogReader createReader();

    boolean hasFullDate();
}
