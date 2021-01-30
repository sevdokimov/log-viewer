package com.logviewer.data2;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class FieldTypes {

    public static final String DATE = "date";

    public static final String LEVEL = "level";
    public static final String LEVEL_LOGBACK = "level/logback";
    public static final String LEVEL_LOG4J = "level/log4j";

    public static final String JAVA_CLASS = "class";

    public static final String MESSAGE = "message";
    public static final String THREAD = "thread";

    public static final String NDC = "ndc";
    
    public static final String MDC = "mdc";

    public static final String PROCESS_ID = "number/processId";

    public static final String NUMBER = "number";

    public static final String USERNAME = "username";

    public static final String HTTP_METHOD = "HTTP_METHOD";

    public static final String NGINX_BYTES_SENT = "number/nginx_bytes_sent";
    public static final String NGINX_REQUEST_TIME = "number/interval_seconds/nginx_request_time";

    public static final String NGINX_REQUEST_ID = "hex/nginx_request_id";

    public static final String NGINX_REMOTE_ADDR = "ip/nginx_remote_addr";

    public static final String NGINX_SCHEME = "nginx_scheme";

    public static final String HOSTNAME = "hostname";

    public static final String USER_AGENT = "text/user-agent";

    /**
     * the number of milliseconds elapsed since the start of the application until the creation of the logging event.
     */
    public static final String RELATIVE_TIMESTAMP = "relativeTimestamp";

    public static final String URI = "uri";

    public static final String NGINX_HTTP_PROTOCOL = "http_protocol";

    public static final String HTTP_RESPONSE_CODE = "number/nginx_response_code";

    public static boolean is(@Nullable String fieldType, @NonNull String expectedType) {
        if (fieldType == null || !fieldType.startsWith(expectedType))
            return false;

        return fieldType.length() == expectedType.length() || fieldType.startsWith("/", expectedType.length());
    }
}
