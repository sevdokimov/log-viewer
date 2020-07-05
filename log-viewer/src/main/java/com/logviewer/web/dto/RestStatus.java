package com.logviewer.web.dto;

import com.google.common.base.Throwables;
import com.logviewer.data2.DirectoryNotVisibleException;
import com.logviewer.data2.LogCrashedException;
import com.logviewer.web.session.LogSession;
import com.logviewer.web.session.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

public class RestStatus {

    private static final Logger log = LoggerFactory.getLogger(RestStatus.class);

    private String error;
    private String errorMessage;
    private String errorType;
    private String hash;

    private long size;
    private long lastModification;

    public RestStatus(Status status) {
        if (status.getHash() != null) {
            this.hash = status.getHash();
            this.size = status.getSize();
            this.lastModification = status.getLastModification();
        }
        else {
            Throwable error = status.getError();
            if (error instanceof NoSuchFileException) {
                errorType = "NoSuchFileException";
            }
            else if (error instanceof AccessDeniedException) {
                errorType = "AccessDeniedException";
            }
            else if (error instanceof DirectoryNotVisibleException) {
                errorType = "DirectoryNotVisibleException";
            }
            else if (error instanceof ConnectException && error.getMessage().equals("Connection refused")) {
                errorType = "ConnectionProblem";
            }
            else if (error instanceof UnresolvedAddressException) {
                errorType = "ConnectionProblem";
                errorMessage = "Unresolved host";
            }
            else if (error == LogSession.NO_DATE_EXCEPTION) {
                errorType = "NoDateField";
            }
            else if (error instanceof IOException) {
                errorType = "IOException";
            }
            else if (error instanceof LogCrashedException) {
                errorType = "LogCrashedException";
            }
            else {
                log.error("Unknown error", error);
                errorType = "error";
            }

            if (errorMessage == null)
                errorMessage = error.getMessage();

            this.error = Throwables.getStackTraceAsString(error);
        }
    }

    public String getError() {
        return error;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }
}
