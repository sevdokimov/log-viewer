package com.logviewer;

/**
 *
 */
public abstract class CommandHandler<T> {

    protected final T argsHolder;

    protected CommandHandler(T argsHolder) {
        this.argsHolder = argsHolder;
    }

    public abstract String getCommandName();

    public abstract void execute() throws Exception;

    public T getArgsHolder() {
        return argsHolder;
    }
}
