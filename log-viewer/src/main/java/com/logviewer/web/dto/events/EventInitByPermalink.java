package com.logviewer.web.dto.events;

import com.logviewer.data2.Position;
import com.logviewer.domain.Permalink;
import com.logviewer.web.session.Status;
import com.logviewer.web.session.tasks.LoadNextResponse;
import com.logviewer.web.session.tasks.SearchPattern;

import java.util.Map;

public class EventInitByPermalink extends DataHolderEvent {

    private final Position selectedLine;
    private final int shiftView;
    private final SearchPattern searchPattern;
    private final boolean hideUnmatched;

    public EventInitByPermalink(Map<String, Status> statuses, long stateVersion, LoadNextResponse data, Permalink permalink) {
        super(statuses, stateVersion, data);

        this.selectedLine = permalink.getSelectedLine();
        this.shiftView = permalink.getShiftView();
        this.searchPattern = permalink.getSearchPattern();
        this.hideUnmatched = permalink.isHideUnmatched();
    }

    @Override
    public String getName() {
        return "onInitByPermalink";
    }
}
