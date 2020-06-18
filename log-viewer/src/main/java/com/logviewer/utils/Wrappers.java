package com.logviewer.utils;

import org.slf4j.Logger;

import java.util.function.BiConsumer;

public class Wrappers {

    public static <A, B> BiConsumer<A, B> of(Logger logger, BiConsumer<A, B> biConsumer) {
        return (a, b) -> {
            try {
                biConsumer.accept(a, b);
            } catch (Throwable e) {
                logger.error("Error", e);
            }
        };
    }
}
