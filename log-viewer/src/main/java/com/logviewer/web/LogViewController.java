package com.logviewer.web;

import com.logviewer.api.LvFilterStorage;
import com.logviewer.api.LvPermalinkStorage;
import com.logviewer.domain.Permalink;
import com.logviewer.utils.LvGsonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.html.FormSubmitEvent;
import java.io.IOException;

public class LogViewController extends AbstractRestRequestHandler {

    @Autowired
    private LvPermalinkStorage permalinkStorage;

    @Autowired
    private LvFilterStorage filterStorage;

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public String generatePermalink(String[] hashAndPermalink) throws IOException {
        String hash = hashAndPermalink[0];
        Permalink permalink = LvGsonUtils.GSON.fromJson(hashAndPermalink[1], Permalink.class);

        if (permalink.getPaths() == null || permalink.getPaths().length == 0
                || permalink.getOffset() == null || permalink.getHashes() == null) {
            throw new IllegalArgumentException();
        }

        return permalinkStorage.save(hash, permalink);
    }

    @Endpoint(method = FormSubmitEvent.MethodType.POST)
    public void saveFilterState(String[] filterStateAndHash) {
        String hash = filterStateAndHash[0];
        String filterState = filterStateAndHash[1];

        filterStorage.saveFilterSet(hash, filterState);
    }
}
