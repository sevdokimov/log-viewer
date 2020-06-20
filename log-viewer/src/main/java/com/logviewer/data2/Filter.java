package com.logviewer.data2;

import com.logviewer.filters.RecordPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Filter {

    private String name;

    private boolean enabled;

    private RecordPredicate predicate;

    public Filter(@Nullable String name, boolean enabled, @Nonnull RecordPredicate predicate) {
        this.name = name;
        this.enabled = enabled;
        this.predicate = predicate;
    }

    public String getName() {
        return name;
    }

    @Nonnull
    public RecordPredicate getPredicate() {
        return predicate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
