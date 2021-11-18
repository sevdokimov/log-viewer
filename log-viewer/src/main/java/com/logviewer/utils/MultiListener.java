package com.logviewer.utils;

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class MultiListener<T> {

    private final Map<Destroyer, T> localListeners = new HashMap<>();

    private final Callable<Destroyer> globalListenerCreator;

    private Destroyer onEmptyNotifier;

    public MultiListener(Callable<Destroyer> globalListenerCreator) {
        this.globalListenerCreator = globalListenerCreator;
    }

    @Nullable
    public synchronized Destroyer addListener(T listener) {
        if (localListeners.isEmpty()) {
            try {
                onEmptyNotifier = globalListenerCreator.call();
            } catch (Exception e) {
                return null;
            }
        }

        Destroyer[] holder = new Destroyer[1];
        Destroyer res = () -> {
            Destroyer destroyer = null;

            synchronized (this) {
                localListeners.remove(holder[0]);
                if (localListeners.isEmpty()) {
                    destroyer = onEmptyNotifier;
                    onEmptyNotifier = null;
                }
            }

            if (destroyer != null)
                destroyer.close();
        };
        holder[0] = res;

        localListeners.put(res, listener);
        return res;
    }

    public synchronized List<T> getListeners() {
        return new ArrayList<>(localListeners.values());
    }
}
