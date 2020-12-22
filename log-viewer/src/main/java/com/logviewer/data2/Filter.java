package com.logviewer.data2;

import com.logviewer.filters.RecordPredicate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class Filter {

    private String name;

    private boolean enabled;

    private RecordPredicate predicate;

    public Filter(@Nullable String name, boolean enabled, @NonNull RecordPredicate predicate) {
        this.name = name;
        this.enabled = enabled;
        this.predicate = predicate;
    }

    public String getName() {
        return name;
    }

    @NonNull
    public RecordPredicate getPredicate() {
        return predicate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
