package com.logviewer.utils;

import com.logviewer.data2.LogFormat;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final long MAX_TIME_MILLIS = 3000000000000L; // 2065-Jan-24

    private static final Pattern SLASHES = Pattern.compile("/{2,}");

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    public static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.wrap(EMPTY_BYTE_ARRAY);

    public static final Object[] EMPTY_OBJECTS = new Object[0];

    private static volatile Path tempDir;

    public static final String LOCAL_HOST_NAME;
    static {
        try {
            String hostname = System.getProperty("log.hostname");
            if (hostname == null) {
                InetAddress localHost = InetAddress.getLocalHost();

                hostname = Boolean.getBoolean("log.canonical.host.name") ? localHost.getCanonicalHostName() : localHost.getHostName();
            }

            LOCAL_HOST_NAME = hostname;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private Utils() {

    }

    public static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {

            }
        }
    }
    
    public static void deleteContent(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @NonNull
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);

                return FileVisitResult.CONTINUE;
            }

            @NonNull
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(path))
                    Files.delete(dir);
                
                return super.postVisitDirectory(dir, exc);
            }
        });

    }

    public static String title(@Nullable String logPath) {
        if (logPath == null || logPath.isEmpty())
            return "";

        if (logPath.indexOf('/') == 0 && logPath.indexOf('\\') == 0)
            return logPath;

        File file = new File(logPath);

        return file.getName() + " in " + file.getParent();
    }

    public static void readFully(ReadableByteChannel channel, ByteBuffer buf, int length) throws IOException {
        while (length > 0) {
            int n = channel.read(buf);
            if (n == -1)
                throw new EOFException();

            length -= n;
        }
    }

    public static void readFully(ReadableByteChannel channel, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int n = channel.read(buf);
            if (n == -1)
                throw new EOFException();
        }
    }

    public static boolean isIdentifier(String s) {
        if (s.length() == 0)
            return false;

        if (!Character.isJavaIdentifierStart(s.charAt(0)))
            return false;

        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i)))
                return false;
        }

        return true;
    }

    public static String toString(ByteBuffer buffer) {
        return toString(buffer, StandardCharsets.UTF_8);
    }

    public static String toString(ByteBuffer buffer, Charset charsets) {
        return new String(buffer.array(), buffer.position(), buffer.limit() - buffer.position(), charsets);
    }

    public static boolean isSubdirectory(String directory, String child) {
        if (directory.equals(child))
            return true;

        if (!directory.endsWith("/")) {
            directory += '/';
        }

        return child.startsWith(directory);
    }

    public static int compareFileNames(@NonNull String f1, @NonNull String f2) {
        Matcher matcher1 = NUMBER.matcher(f1);
        Matcher matcher2 = NUMBER.matcher(f2);

        int idx = 0;
        while (true) {
            if (matcher1.find() && matcher2.find()) {
                if (matcher1.start() == matcher2.start()) {
                    int res = String.CASE_INSENSITIVE_ORDER.compare(f1.substring(idx, matcher1.start()), f2.substring(idx, matcher1.start()));
                    if (res != 0)
                        return res;

                    if (matcher1.group().equals(matcher2.group())) {
                        idx = matcher1.end();
                        continue;
                    }

                    return new BigInteger(matcher1.group()).compareTo(new BigInteger(matcher2.group()));
                }
            }

            break;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(f1.substring(idx), f2.substring(idx));
    }

    public static boolean containsIgnoreCase(final String str, final String searchStr) {
        if (str == null || searchStr == null)
            return false;
        if (searchStr.length() == 0)
            return true;

        final int len = searchStr.length();
        final int max = str.length() - len;

        char a = searchStr.charAt(0);
        char firstLetterUp = Character.toUpperCase(a);
        char firstLetterLow = Character.toLowerCase(a);

        for (int i = 0; i <= max; i++) {
            a = str.charAt(i);
            if ((a == firstLetterLow || a == firstLetterUp) && str.regionMatches(true, i + 1, searchStr, 1, len - 1)) {
                return true;
            }
        }

        return false;
    }

    public static <T> int indexOf(List<? extends T> list, Predicate<T> predicate) {
        int idx = 0;
        for (T t : list) {
            if (predicate.test(t))
                return idx;

            idx++;
        }

        return -1;
    }

    public static Long getFormatHash(LogFormat format) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Utils.putUnencodedChars(digest, LvGsonUtils.GSON.toJson(format));
            return ByteBuffer.wrap(digest.digest()).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T safeGet(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        } catch (ExecutionException e) {
            throw propagate(e.getCause());
        }
    }

    public static String normalizePath(String path) {
        path = path.replace('\\', '/');
        return SLASHES.matcher(path).replaceAll("/");
    }

    public static RuntimeException propagate(@NonNull Throwable t) {
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        if (t instanceof Error)
            throw (Error) t;

        throw new RuntimeException(t);
    }

    public static void putUnencodedChars(MessageDigest digest, String s) {
        for (int i = 0, len = s.length(); i < len; i++) {
            putUnencodedChars(digest, s.charAt(i));
        }
    }

    public static void putUnencodedChars(MessageDigest digest, char c) {
        digest.update((byte) c);
        digest.update((byte) (c >>> 8));
    }

    public static void putInt(MessageDigest digest, int x) {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putInt(x);
        digest.update(buff.array());
    }

    public static <K, V> Map<K, V> newMap(Object ... keysAndValues) {
        assert (keysAndValues.length & 1) == 0;

        Map res = new LinkedHashMap<>();

        for (int i = 0; i < keysAndValues.length; i += 2) {
            res.put(keysAndValues[i], keysAndValues[i + 1]);
        }

        return res;
    }

    @NonNull
    public static String getStackTraceAsString(@NonNull Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static void assertValidTimestamp(long nano) {
        if (nano <= 0)
            return;

        if (nano < MAX_TIME_MILLIS)
            throw new IllegalArgumentException("Time must be specified in nanoseconds, but looks like it is milliseconds: " + nano);
    }

    @NonNull
    public static Path getTempDir() throws IOException {
        Path res = tempDir;
        if (res == null) {
            res = Paths.get(System.getProperty("java.io.tmpdir"), "log-viewer");
            Files.createDirectories(res);

            tempDir = res;
        }

        return res;
    }

    public static String removeAsciiColorCodes(String s) {
//        return s.replaceAll("\u001B\\[[\\d;]*m", "");    - don't use the regexp, the performance of the regexp is not good.

        StringBuilder res = null;

        int i = 0;

        while (i < s.length()) {
            int escapeIdx = s.indexOf('\u001B', i);
            if (escapeIdx < 0)
                break;

            if (escapeIdx + 2 < s.length()) {
                if (s.charAt(escapeIdx + 1) == '[') {
                    int k = escapeIdx + 2;
                    char a;

                    do {
                        a = s.charAt(k);

                        if (a == ';' || (a >= '0' && a <= '9')) {
                            k++;

                            if (k >= s.length())
                                break;

                            continue;
                        }

                        break;
                    } while (true);

                    if (a == 'm') {
                        if (res == null) {
                            res = new StringBuilder(s.length());
                            res.append(s, 0, escapeIdx);
                        } else {
                            res.append(s, i, escapeIdx);
                        }

                        i = k + 1;
                        continue;
                    }
                }
            }

            if (res != null)
                res.append(s, i, escapeIdx + 1);

            i = escapeIdx + 1;
        }

        if (res == null)
            return s;

        res.append(s, i, s.length());

        return res.toString();
    }
}
