package com.logviewer.utils;

import com.google.common.base.Throwables;
import com.google.common.hash.Hashing;
import com.google.gson.JsonParser;
import com.logviewer.data2.LogFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
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
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    public static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.wrap(EMPTY_BYTE_ARRAY);

    public static final Object[] EMPTY_OBJECTS = new Object[0];

    public static final JsonParser parser = new JsonParser();

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
            @Nonnull
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);

                return FileVisitResult.CONTINUE;
            }

            @Nonnull
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

    public static int compareFileNames(@Nonnull String f1, @Nonnull String f2) {
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

    public static String getFormatHash(LogFormat format) {
        return Hashing.md5().hashUnencodedChars(LvGsonUtils.GSON.toJson(format)).toString();
    }

    public static <T> T safeGet(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        } catch (ExecutionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }
}
