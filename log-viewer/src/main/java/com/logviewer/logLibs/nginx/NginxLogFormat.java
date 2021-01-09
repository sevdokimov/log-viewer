package com.logviewer.logLibs.nginx;

import com.logviewer.data2.FieldTypes;
import com.logviewer.formats.AbstractPatternLogFormat;
import com.logviewer.formats.utils.*;
import org.springframework.lang.NonNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NginxLogFormat extends AbstractPatternLogFormat {

    private static final Pattern ITEM = Pattern.compile("(\\$[a-z0-9_]+)|([^$]+)");

    private static final String[] HTTP_METHODS = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"};

    public NginxLogFormat(@NonNull String pattern) {
        this(null, pattern);
    }

    public NginxLogFormat(Charset charset, @NonNull String pattern) {
        super(charset, pattern);
    }

    @Override
    protected LvLayoutNode[] parseLayout(@NonNull String pattern) throws IllegalArgumentException {
        List<LvLayoutNode> res = new ArrayList<>();

        Matcher matcher = ITEM.matcher(pattern);

        while (matcher.lookingAt()) {
            String field = matcher.group(1);

            if (field != null) {
                res.add(field(field.substring(1)));
            } else {
                res.add(new LvLayoutTextNode(matcher.group()));
            }

            matcher.region(matcher.end(), pattern.length());
        }

        mergeMessageFields(res);

        return res.toArray(new LvLayoutNode[0]);
    }

    private LvLayoutNode field(String name) {
        switch (name) {
            case "bytes_sent":
                return new NginxOptionalNumber(name, FieldTypes.NGINX_BYTES_SENT);

            case "body_bytes_sent":
                return new NginxOptionalNumber(name, FieldTypes.NGINX_BYTES_SENT);

            case "remote_addr":
                return new LvLayoutIpNode(name, FieldTypes.NGINX_BYTES_SENT);

            case "remote_user":
                return new NginxStretchNode(name, FieldTypes.USERNAME, true, 1);

            case "time_local":
                return new LvLayoutSimpleDateNode("dd/MMM/yyyy:HH:mm:ss Z");

            case "request_method":
                return new LvLayoutFixedTextNode(name, FieldTypes.HTTP_METHOD, HTTP_METHODS);

            case "scheme":
                return new LvLayoutFixedTextNode(name, FieldTypes.NGINX_SCHEME, "http", "https");

            case "host":
            case "hostname":
                return new LvLayoutRegexNode(name, FieldTypes.HOSTNAME, "(?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*(?:[A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])");

            case "request":
            case "request_uri":
                return new NginxUriNode(name);

            case "server_protocol":
                return new LvLayoutRegexNode(name, FieldTypes.NGINX_HTTP_PROTOCOL, Pattern.compile("HTTP/\\d\\.\\d"));

            case "status":
                return new NginxOptionalNumber(name, FieldTypes.HTTP_RESPONSE_CODE);

            case "http_referer":
                return new NginxUriNode(name);

            case "http_user_agent":
                return new NginxStretchPatternNode(name, FieldTypes.USER_AGENT, true,
                        Pattern.compile("[a-z\\-]+/\\d+\\.\\d+(?:\\.\\d+)? *(?:\\([\\w ,.;\\-/]+\\))?", Pattern.CASE_INSENSITIVE));

            case "request_time":
                return new LvLayoutNumberNode(name, FieldTypes.NGINX_REQUEST_TIME, false, true);

            case "request_id":
                return new LvLayoutHexNode(name, FieldTypes.NGINX_REQUEST_ID, 30);

            case "any":
                return new LvLayoutStretchNode("message", FieldTypes.MESSAGE, false, 0);

//            case "connection":
//                return new NginxOptionalNumber(name, FieldTypes.NGINX_BYTES_SENT);
//
//            case "connection_requests":
//                return new NginxOptionalNumber(name, FieldTypes.NGINX_BYTES_SENT);



            default:
                throw new IllegalArgumentException("Unexpected field: $" + name);
        }
    }
}
