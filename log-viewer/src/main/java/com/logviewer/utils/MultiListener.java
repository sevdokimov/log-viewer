package com.logviewer.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MultiListener<T> {

    private final Map<Destroyer, T> localListeners = new HashMap<>();

    private final Supplier<Destroyer> globalListenerCreator;

    private Destroyer onEmptyNotifier;

    public MultiListener(Supplier<Destroyer> globalListenerCreator) {
        this.globalListenerCreator = globalListenerCreator;
    }

    public Destroyer addListener(T listener) {
        boolean isFirstListener;

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

        synchronized (this) {
            isFirstListener = localListeners.isEmpty();

            localListeners.put(res, listener);
        }

        if (isFirstListener) {
            onEmptyNotifier = globalListenerCreator.get();
        }

        return res;
    }

    public synchronized List<T> getListeners() {
        return new ArrayList<>(localListeners.values());
    }
}
