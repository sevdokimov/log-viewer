package com.logviewer.web.dto;

import com.logviewer.data2.DirectoryNotVisibleException;
import com.logviewer.data2.LogCrashedException;
import com.logviewer.utils.Utils;
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

    /**
     * See log-file.ts/ErrorType
     */
    private String errorType;
    private String errorMessage;
    private String detailedErrorMessage;

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
            Throwable exception = status.getError();
            if (exception instanceof NoSuchFileException) {
                errorType = "NoSuchFileException";
            }
            else if (exception instanceof AccessDeniedException) {
                errorType = "AccessDeniedException";
            }
            else if (exception instanceof DirectoryNotVisibleException) {
                errorType = "DirectoryNotVisibleException";
            }
            else if (exception instanceof ConnectException && exception.getMessage().equals("Connection refused")) {
                errorType = "ConnectionProblem";
            }
            else if (exception instanceof UnresolvedAddressException) {
                errorType = "ConnectionProblem";
                errorMessage = "Unresolved host";
            }
            else if (exception == LogSession.NO_DATE_EXCEPTION) {
                errorType = "NoDateField";
            }
            else if (exception instanceof IOException) {
                errorType = "IOException";
                detailedErrorMessage = Utils.getStackTraceAsString(exception);
            }
            else if (exception instanceof LogCrashedException) {
                errorType = "LogCrashedException";
            }
            else {
                log.error("Unknown error", exception);
                errorType = "internal_error";
                detailedErrorMessage = Utils.getStackTraceAsString(exception);
            }

            if (errorMessage == null)
                errorMessage = exception.getMessage();
        }
    }

    public String getDetailedErrorMessage() {
        return detailedErrorMessage;
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
