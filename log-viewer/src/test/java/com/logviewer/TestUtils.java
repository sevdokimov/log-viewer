package com.logviewer;

import com.google.common.base.Throwables;
import com.logviewer.data2.FieldTypes;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.Record;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.web.dto.RestRecord;
import org.junit.Assert;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestUtils {

    public static final LogFormat MULTIFILE_LOG_FORMAT = new RegexLogFormat(StandardCharsets.UTF_8,
            "(150101 10:\\d\\d:\\d\\d) (.*)", false,
            "yyMMdd HH:mm:ss", "date",
            new RegexLogFormat.RegexField("date", 1, FieldTypes.DATE),
            new RegexLogFormat.RegexField("msg", 2, "message")
    );

    private static final Pattern MULTIFILE_LOG_RECORD_PATTERN = Pattern.compile("(\\d{6} \\d\\d:\\d\\d:\\d\\d) (.*)");

    public static final long WAIT_TIMEOUT = Integer.getInteger("test-wait-timeout", 5) * 1000;

    public static void assertEquals(Record r1, Record r2) {
        Assert.assertEquals(r1.getMessage(), r2.getMessage());
        Assert.assertEquals(r1.getStart(), r2.getStart());
        Assert.assertEquals(r1.getEnd(), r2.getEnd());
        Assert.assertEquals(r1.hasMore(), r2.hasMore());

        Assert.assertEquals(r1.getFieldsCount(), r2.getFieldsCount());

        for (int i = 0; i < r1.getFieldsCount(); i++) {
            Assert.assertEquals(r1.getFieldStart(i), r2.getFieldStart(i));
            Assert.assertEquals(r1.getFieldEnd(i), r2.getFieldEnd(i));
        }
    }

    public static void assertEquals(List<Record> list1, List<Record> list2) {
        Assert.assertEquals(list1.size(), list2.size());

        for (int i = 0; i < list1.size(); i++) {
            assertEquals(list1.get(i), list2.get(i));
        }
    }

    public static void assertUnparsed(Record record, String content) {
        Assert.assertEquals(content, record.getMessage());

        for (int i = 0; i < record.getFieldsCount(); i++) {
            Assert.assertNull(record.getFieldText(i));
            Assert.assertEquals(-1, record.getFieldStart(i));
            Assert.assertEquals(-1, record.getFieldEnd(i));
        }
    }

    public static void check(List<Record> res, String ... expected) {
        Assert.assertEquals(Arrays.asList(expected), res.stream().map(Record::getMessage).collect(Collectors.toList()));
    }

    public static void check(Collection<RestRecord> res, String ... expected) {
        Assert.assertEquals(Arrays.asList(expected), res.stream().map(RestRecord::getText).collect(Collectors.toList()));
    }

    public static <T> void checkOrder(Collection<T> list, Comparator<T> c) {
        T prev = null;

        for (T t : list) {
            assert prev == null || c.compare(prev, t) <= 0 : list;

            prev = t;
        }
    }

    public static String[] createMultifileLog(String file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String s;

            Map<String, StringBuilder> builderMap = new HashMap<>();

            while ((s = br.readLine()) != null) {
                s = s.trim();
                if (s.length() == 0 || s.startsWith("#"))
                    continue;

                Matcher matcher = MULTIFILE_LOG_RECORD_PATTERN.matcher(s);
                if (!matcher.find())
                    throw new IllegalArgumentException(s);

                String msg = matcher.group(2);
                String fileName = msg.substring(msg.lastIndexOf(' ') + 1);

                StringBuilder sb = builderMap.computeIfAbsent(fileName, k -> new StringBuilder());
                if (sb.length() > 0)
                    sb.append('\n');

                sb.append(s);
            }

            List<String> res = new ArrayList<>();

            for (Map.Entry<String, StringBuilder> entry : builderMap.entrySet()) {
                File f = File.createTempFile("log-viewer/test/" + entry.getKey() + "-", ".log");
                f.deleteOnExit();
                res.add(f.getPath());

                Files.write(f.toPath(), entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
            }

            return res.toArray(new String[0]);
        }
    }

    public static long date(int mm, int ss) {
        return new Date(115, 0, 1, 10, mm, ss).getTime();
    }

    public static <T> void assertEqualsUnorder(@NonNull  Collection<T> c, T ... objects) {
        Assert.assertEquals(new HashSet<>(Arrays.asList(objects)), new HashSet<>(c));
    }

    public static <T, R> void assertEqualsUnorder(@NonNull Collection<T> c, @NonNull Function<T, R> transformer, R ... objects) {
        Assert.assertEquals(new HashSet<>(Arrays.asList(objects)), c.stream().map(transformer).collect(Collectors.toSet()));
    }

    public static <T extends Throwable> T assertError(Class<T> error, ExceptionalRunnable run) {
        try {
            run.run();

            throw new RuntimeException("Exception expected: " + error);
        } catch (Throwable t) {
            if (error.isInstance(t))
                return (T) t;

            throw Throwables.propagate(t);
        }
    }

    public interface ExceptionalRunnable {
        void run() throws Exception;
    }
}
