package com.logviewer.formats.utils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastDateTimeParser implements BiFunction<String, ParsePosition, Supplier<Instant>> {

    private static final Pattern TIME_PATTERN = Pattern.compile("(.*?)(?:z+|Z+|X+)");

    public static final Map<String, TimeZone> ALL_ZONES;

    private static final Method parseUnresolved0Method;
    private static final Method toResolvedMethod;
    static {
        try {
            parseUnresolved0Method = DateTimeFormatter.class.getDeclaredMethod("parseUnresolved0", CharSequence.class, ParsePosition.class);
            parseUnresolved0Method.setAccessible(true);

            Class<?> ctxClass = parseUnresolved0Method.getReturnType();
            toResolvedMethod = ctxClass.getDeclaredMethod("toResolved", ResolverStyle.class, Set.class);
            toResolvedMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        ALL_ZONES = new HashMap<>();

        for (String zoneId : TimeZone.getAvailableIDs()) {
            TimeZone zone = TimeZone.getTimeZone(zoneId);

            ALL_ZONES.put(zone.getDisplayName(true, TimeZone.SHORT), zone);
            ALL_ZONES.put(zone.getDisplayName(false, TimeZone.SHORT), zone);
        }
    }

    /**
     * See https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8031085
     */
    private static final boolean isJDK8031085fixed;
    static {
        boolean parsedSuccessfully;
        try {
            Instant.from(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").parse("20210505112233999"));
            parsedSuccessfully = true;
        } catch (DateTimeException e) {
            parsedSuccessfully = false;
        }

        isJDK8031085fixed = parsedSuccessfully;
    }

    @NonNull
    private final DateTimeFormatter formatter;
    private final boolean hasTimezone;

    private final TimeZone defaultTimeZone;

    private transient String lastTimezoneStr;
    private transient TimeZone lastTimeZone;

    private FastDateTimeParser(@NonNull String pattern, boolean hasTimezone, @Nullable TimeZone defaultTimeZone) {
        formatter = DateTimeFormatter.ofPattern(pattern);
        this.hasTimezone = hasTimezone;
        this.defaultTimeZone = defaultTimeZone == null ? TimeZone.getDefault() : defaultTimeZone;
    }

    @Override
    public Supplier<Instant> apply(String s, ParsePosition pos) {
        TemporalAccessor res = parseTimestampWithoutZone(formatter, s, pos);
        if (res == null)
            return null;

        TimeZone zone;

        if (hasTimezone) {
            zone = getTimezone(s, pos);
            if (zone == null)
                return null;
        } else {
            zone = defaultTimeZone;
        }

        return () -> {
            if (!res.isSupported(ChronoField.INSTANT_SECONDS)) {
                LocalDateTime localDateTime = LocalDateTime.from(res);
                return localDateTime.atZone(zone.toZoneId()).toInstant();
            } else {
                return Instant.from(res);
            }
        };
    }

    /**
     * Parse the following timezone formats: "Z", "+0300", "-04:00", "GMT+0600", "GMT+01:00", "GMT+03", "MSK"
     */
    @Nullable
    private TimeZone getTimezone(String s, ParsePosition pos) {
        int idx = pos.getIndex();

        if (lastTimezoneStr != null && s.startsWith(lastTimezoneStr, idx)) {
            pos.setIndex(idx + lastTimezoneStr.length());
            return lastTimeZone;
        }

        TimeZone res = parseTimezone(s, pos);
        if (res != null) {
            lastTimezoneStr = s.substring(idx, pos.getIndex());
            lastTimeZone = res;
        }

        return res;
    }

    @Nullable
    public static TimeZone parseTimezone(String s, ParsePosition pos) {
        int idx = pos.getIndex();

        if (idx >= s.length())
            return null;

        char a = s.charAt(idx);
        if (a == 'Z') {  // "Z" means GMT
            pos.setIndex(idx + 1);
            return TimeZone.getTimeZone("GMT");
        }

        if (a == '-' || a == '+') {
            return parseOffset(s, idx, pos);
        }

        if (isUpperLetter(a) && idx + 3 <= s.length() && isUpperLetter(s.charAt(idx + 1)) && isUpperLetter(s.charAt(idx + 2))) {
            if (idx + 3 == s.length() || !Character.isLetter(s.charAt(idx + 3))) {
                String id = s.substring(idx, idx + 3);

                if (id.equals("GMT") && idx + 3 < s.length() && (s.charAt(idx + 3) == '-' || s.charAt(idx + 3) == '+')) {
                    TimeZone res = parseOffset(s, idx + 3, pos);
                    if (res != null)
                        return res;
                }

                pos.setIndex(idx + 3);

                return ALL_ZONES.get(id);
            }
        }

        return null;
    }

    private static boolean isUpperLetter(char a) {
        return a >= 'A' && a <= 'Z';
    }

    /**
     * parse offset like "+0300", "-04:00"
     */
    @Nullable
    private static TimeZone parseOffset(String s, int idx, ParsePosition pos) {
        if (idx + 3 > s.length())
            return null;

        char h1 = s.charAt(idx + 1);
        char h2 = s.charAt(idx + 2);

        if (h1 == '0') {
            if (h2 < '0' || h2 > '9')
                return null;
        } else if (h1 == '1') {
            if (h2 < '0' || h2 > '2')
                return null;
        } else {
            return null;
        }

        if (idx + 6 <= s.length() && s.charAt(idx + 3) == ':' && (s.startsWith("00", idx + 4) || s.startsWith("30", idx + 4)) && isNotDigit(s, idx + 6)) {
            pos.setIndex(idx + 6);
            return TimeZone.getTimeZone("GMT" + s.substring(idx, idx + 6));
        }

        if (idx + 5 <= s.length() && (s.startsWith("00", idx + 3) || s.startsWith("30", idx + 3)) && isNotDigit(s, idx + 5)) {
            pos.setIndex(idx + 5);
            return TimeZone.getTimeZone("GMT" + s.substring(idx, idx + 5));
        }

        if (isNotDigit(s, idx + 3)) {
            pos.setIndex(idx + 3);
            return TimeZone.getTimeZone("GMT" + s.substring(idx, idx + 3) + "00");
        }

        return null;
    }

    private static boolean isNotDigit(String s, int offset) {
        if (offset >= s.length())
            return true;

        char a = s.charAt(offset);
        return a < '0' || a > '9';
    }

    @Nullable
    private static TemporalAccessor parseTimestampWithoutZone(DateTimeFormatter formatter, String s, ParsePosition parsePosition) {
        try {
            Object context = parseUnresolved0Method.invoke(formatter, s, parsePosition);
            if (context == null || parsePosition.getErrorIndex() >= 0)
                return null;

            return (TemporalAccessor) toResolvedMethod.invoke(context, formatter.getResolverStyle(), formatter.getResolverFields());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof DateTimeException || target instanceof IndexOutOfBoundsException) {
                return null;
            }

            if (target instanceof RuntimeException)
                throw (RuntimeException)target;

            throw new RuntimeException(e);
        }
    }

    private static BiFunction<String, ParsePosition, Supplier<Instant>> simpleDateFormatter(@NonNull SimpleDateFormat format) {
        return (s, pos) -> {
            Date parsed = format.parse(s, pos);
            if (parsed == null)
                return null;

            return () -> parsed.toInstant();
        };
    }

    public static BiFunction<String, ParsePosition, Supplier<Instant>> createFormatter(@NonNull String pattern,
                                                                                       @Nullable TimeZone defaultTimeZone) throws IllegalArgumentException {
        if (!isJDK8031085fixed) {
            int idx = pattern.indexOf("sSSS");
            if (idx >= 0) {
                // microseconds and nanoseconds are not supported if there is no separator before.
                if (pattern.startsWith("sSSSS", idx))
                    throw new IllegalArgumentException("The date pattern is not supported: " + pattern);

                // DateTimeFormatter doesn't support pattern like "yyyyMMddHHmmssSSS", see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8031085
                // use SimpleDateFormat as a workaround
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                if (defaultTimeZone != null)
                    format.setTimeZone(defaultTimeZone);
                
                return simpleDateFormatter(format);
            }
        }

        Matcher matcher = TIME_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return new FastDateTimeParser(pattern, false, defaultTimeZone);
        }

        return new FastDateTimeParser(matcher.group(1), true, defaultTimeZone);
    }

}
