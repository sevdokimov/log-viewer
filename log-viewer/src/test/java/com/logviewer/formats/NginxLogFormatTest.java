package com.logviewer.formats;

import com.logviewer.AbstractLogTest;
import com.logviewer.TestUtils;
import com.logviewer.data2.BufferedFile;
import com.logviewer.data2.LogReader;
import com.logviewer.data2.LogRecord;
import com.logviewer.logLibs.nginx.NginxLogFormat;
import org.junit.Test;
import org.springframework.lang.NonNull;

public class NginxLogFormatTest extends AbstractLogTest {

    private static final String[] EXAMPLES = {
            "$remote_addr - $remote_user [$time_local] \"$request_method $scheme://$host$request_uri $server_protocol\" $status $bytes_sent \"$http_referer\" \"$http_user_agent\" $request_time - \"$request_id\"'",
            "$time_iso8601 sn=\"$connection\" ips=\"$remote_addr_ipscrub\" rm=\"$request_method\" sa=\"$server_addr\" r=\"$request\" u=\"$uri\" ru=\"$request_uri\" q=\"$query_string\" url=\"$scheme://$host$request_uri\" rl=\"$request_length\" rt=\"$request_time\" bbs=\"$body_bytes_sent\" gzr=\"$gzip_ratio\" ups=\"$upstream_status\" upct=\"$upstream_connect_time\" uprt=\"$upstream_response_time\" uprl=\"$upstream_response_length\" upbr=\"$upstream_bytes_received\" upbs=\"$upstream_bytes_sent\" srvp=\"$server_protocol\" tlsp=\"$ssl_protocol\" tlsc=\"$ssl_cipher\" tlsed=\"$ssl_early_data\" ref=\"$http_referer\" hua=\"$http_user_agent\" hxf=\"$http_x_forwarded_for\"",
            "$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\" - \"$request_id\"",
            "$request_time $upstream_response_time $remote_addr $request_length $upstream_addr  [$time_local] \"$http_referer\" \"$http_user_agent\" \"$gzip_ratio\" \"$http_x_forwarded_for\" - \"$server_addr:$server_port $cookie_aQQ_ajkguid\"",
            "$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\" \"$http_x_forwarded_for\"",
            "$remote_addr | $remote_user | $time_local | $request | $http_accept_encoding | $http_user_agent | $http_x_forwarded_for | $request_time | $upstream_response_time",
            "$hostname $remote_addr - $remote_user - \"$http_x_forwarded_for\" - [$time_local] \"$request\" : \"$request_body\"",
            "$ssl_protocol/$ssl_cipher $remote_addr - \"$http_x_forwarded_for\"  - $remote_user [$time_local] \"$request\"",
            "$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\" \"$http_x_forwarded_for\"",
            "$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\" \"$http_x_forwarded_for\"",
            "$http_host $remote_addr [$time_local] \"$request\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\" $request_time $upstream_response_time (xff: $http_x_forwarded_for)",
    };

    @Test
    public void unsupportedPattern() {
        NginxLogFormat format = new NginxLogFormat("$zzz");

        TestUtils.assertError(IllegalArgumentException.class, format::createReader);
    }

//    @Test
//    public void parsing() {
//        for (String example : EXAMPLES) {
//            NginxLogFormat format = new NginxLogFormat(example);
//            format.createReader();
//        }
//    }

    @Test
    public void testNginx() {
        check("$remote_addr ($bytes_sent)", "8.8.8.8 (23)", "8.8.8.8", "23");

        check("$remote_addr - $remote_user [$time_local]", "18.18.18.8 - - [10/Oct/2000:13:55:36 -0700]",
                "18.18.18.8", "-", "10/Oct/2000:13:55:36 -0700");

        check("$remote_addr - $remote_user [$time_local]", "18.18.18.8 - john smith [10/Oct/2000:13:55:36 -0700]",
                "18.18.18.8", "john smith", "10/Oct/2000:13:55:36 -0700");

        check("$remote_user $request_method", "POST GET GET", "POST GET", "GET");
        check("$remote_user $request_method", "GET HEAD", "GET", "HEAD");
        failed("$remote_user $request_method", "POST aaa");

        check("$request_method $remote_user", "POST aaa", "POST", "aaa");
        failed("$request_method $remote_user ", "POST aa\\ ");
        check("$request_method $remote_user     ", "POST aa\\u0000     ", "POST", "aa\\u0000");

        check("$remote_user\"$request_uri\"", "a\"a\"a\"/\"", "a\"a\"a", "/");

        failed("$remote_user$request_uri", "||/aaa ");
        check("$remote_user$request_uri", "||/aaa", "||", "/aaa");
        check("$remote_user $request_uri", "aa /aaa", "aa", "/aaa");
        check("$remote_user $request_uri", "aa     /aaa", "aa", "/aaa");
        check("\"$request_uri\"$remote_user", "\"/a\\\"aa\"uuu", "/a\\\"aa", "uuu");

        check("$server_protocol ", "HTTP/1.0 ", "HTTP/1.0");
        check("-$server_protocol", "-HTTP/1.1", "HTTP/1.1");
        check("-$server_protocol-", "-HTTP/2.0-", "HTTP/2.0");
        failed("$server_protocol", "HTTP/a.0");
        check("$server_protocol $status", "HTTP/1.0 200", "HTTP/1.0", "200");

        check("$remote_addr - $remote_user [$time_local] \"$request_method $request_uri $server_protocol\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\"",
                "195.154.122.76 - - [01/Jan/2021:00:21:07 +0300] \"GET /ideaPlugin.html HTTP/1.1\" 200 2366 \"-\" \"Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)\"",

                "195.154.122.76",
                "-",
                "01/Jan/2021:00:21:07 +0300",
                "GET",
                "/ideaPlugin.html",
                "HTTP/1.1",
                "200",
                "2366",
                "-",
                "Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)"
        );

        check("$remote_addr - $remote_user [$time_local] \"$request_method $request_uri $server_protocol\" $status $body_bytes_sent \"$http_referer\" \"$http_user_agent\"",
                "34.199.145.87 - - [01/Jan/2021:00:19:25 +0300] \"GET /ads.txt HTTP/1.1\" 200 78 \"-\" \"Apache-HttpClient/4.5.9 (Java/1.8.0_201)\"",

                "34.199.145.87",
                "-",
                "01/Jan/2021:00:19:25 +0300",
                "GET",
                "/ads.txt",
                "HTTP/1.1",
                "200",
                "78",
                "-",
                "Apache-HttpClient/4.5.9 (Java/1.8.0_201)"
        );
    }

    private void check(@NonNull String pattern, String event, String ... fields) {
        NginxLogFormat format = new NginxLogFormat(pattern);

        LogRecord record = read(format, event);
        checkFields(record, fields);
    }

    private void failed(@NonNull String pattern, String event, String ... fields) {
        NginxLogFormat format = new NginxLogFormat(pattern);

        LogReader reader = format.createReader();
        boolean isSuccess = reader.parseRecord(new BufferedFile.Line(event));

        assert !isSuccess : event;
    }

}
