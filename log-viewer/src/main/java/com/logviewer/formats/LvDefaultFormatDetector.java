package com.logviewer.formats;

import com.logviewer.data2.LogFormat;
import com.logviewer.formats.utils.FastDateTimeParser;
import com.logviewer.logLibs.log4j.Log4jLogFormat;
import com.logviewer.logLibs.nginx.NginxLogFormat;
import com.logviewer.utils.Pair;
import com.logviewer.utils.TextRange;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LvDefaultFormatDetector {

    private static final int WINDOW_SIZE = 8 * 1024;

    static final String UNKNOWN_FORMAT = "???";

    private static final String TZ_PATTERN = "(?:(?<tzSeparator> )?(?<timezone>Z|(?:GMT)?[-+](?:0\\d|1[0-2])(?::?[03]0)?|"
            + String.join("|", FastDateTimeParser.ALL_ZONES.keySet()) + "))?";

    private static final String MS_PATTERN = "(?:(?<msSeparator>[,.])?(?<ms>\\d{3}(\\d{6}|\\d{3}|\\d)?))?"; // valid milliseconds is "SSS", "SSSS", "SSSSSS", "SSSSSSSSS"

    private static final String MS_TZ = MS_PATTERN + TZ_PATTERN;

    private static final Pattern DATE_ISO8601 = Pattern.compile("\\b20[012]\\d([-/])(?:1[12]|0\\d)\\1(?:[012]\\d|3[10])(?<timeSep>[ _T])(?:0\\d|1\\d|2[0-3]):[0-5]\\d:[0-5]\\d" + MS_TZ + "\\b");

    private static final Pattern DATE_COMPACT = Pattern.compile("\\b20[012]\\d(?:1[12]|0\\d)(?:[012]\\d|3[10])([ _T]?)(?:0\\d|1\\d|2[0-3])[0-5]\\d[0-5]\\d" + MS_TZ + "\\b");

    private static final Pattern DATE_LONG = Pattern.compile("\\b(?:[012]\\d|3[10]) (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) 20[012]\\d (?:0\\d|1\\d|2[0-3]):[0-5]\\d:[0-5]\\d" + MS_TZ + "\\b");

    private static final Pattern DATE_LONG_2 = Pattern.compile("\\b20[012]\\d(?<dateSep>[ -])(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\1(?:[012]\\d|3[10])(?<dtSep>[ _])(?:0\\d|1\\d|2[0-3]):[0-5]\\d:[0-5]\\d" + MS_TZ + "\\b");

    private static final Pattern TIME_WITHOUT_DATE = Pattern.compile("\\b(?:0\\d|1\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\b");

    private static final Pattern LEVEL = Pattern.compile("\\b(?:ERROR|WARN|INFO|DEBUG|TRACE|SEVERE|WARNING|CONFIG|FINE|FINER|FINEST|FATAL)\\b");

    private static final Pattern NGINX_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3} - [^\\[\\]\\s]+ \\[\\d\\d/(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)/20[012]\\d:\\d\\d:\\d\\d:\\d\\d [+-][012]\\d[03]0] ");

    private static final Pattern THREAD_ITEM = Pattern.compile(" *(\\[[^\\[\\]\n]+]) *");

    private static final Pattern SPRING_PATTERN = Pattern.compile("\\b20[012]\\d-(?:1[12]|0\\d)-(?:[012]\\d|3[10]) (?:0\\d|1\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\.\\d\\d\\d +" + LEVEL.pattern() + " +\\d{2,7} --- \\[.+\\] +(?:\\w+\\.)*\\w+ +: .+");

    private static final String SPRING_LOG4J_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %p %processId --- [%t] %logger : %m%n";

    private static int readBuffer(@NonNull Path path, byte[] data) {
        int length = 0;

        try (FileInputStream in = new FileInputStream(path.toFile())) {
            while (length < data.length) {
                int n = in.read(data, length, data.length - length);
                if (n < 0)
                    return length;

                length += n;
            }

            return length;
        } catch (IOException e) {
            return -1;
        }
    }

    private static int findLineEnd(byte[] data, int length) {
        if (length < data.length) {
            return length;
        }

        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] == '\n')
                return i;
        }

        return 0;
    }

    public static LogFormat detectFormat(@NonNull Path path) {
        byte[] data = new byte[WINDOW_SIZE];
        int length = readBuffer(path, data);
        if (length <= 0)
            return null;

        length = findLineEnd(data, length);
        if (length <= 0) {
            return null;
        }

        String s = new String(data, 0, length, StandardCharsets.UTF_8);

        Map<Pair<Boolean, String>, Integer> map = new HashMap<>();

        for (StringTokenizer st = new StringTokenizer(s, "\n"); st.hasMoreTokens(); ) {
            String line = st.nextToken();
            if (line.trim().isEmpty() || line.startsWith("\tat") || line.startsWith("Caused by: "))
                continue;

            Pair<Boolean, String> format = detectFormatOfLine(line);
            if (format != null) {
                map.compute(format, (key, val) -> val == null ? 1 : val + 1);
            }
        }

        if (map.isEmpty())
            return null;

        Pair<Boolean, String> format = Collections.max(map.entrySet(), Map.Entry.comparingByValue()).getKey();
        if (format == null || format.getSecond().equals(UNKNOWN_FORMAT))
            return null;

        if (map.get(format) > map.values().stream().mapToInt(x -> x).sum() * 2 / 3) {
            if (format.getFirst()) {
                return new Log4jLogFormat(null, format.getSecond(), false);
            } else {
                return new NginxLogFormat(format.getSecond());
            }
        }

        return null;
    }

    private static TextRange expandRange(String line, TextRange range) {
        int end = range.getEnd();
        while (true) {
            if (end >= line.length())
                return range;

            if (line.charAt(end) != ' ')
                break;

            end++;
        }

        if (line.charAt(end) != ']')
            return range;

        int start = range.getStart();
        while (true) {
            if (start < 0)
                return range;

            start--;

            if (line.charAt(start) != ' ')
                break;
        }

        if (line.charAt(start) != '[')
            return range;

        return new TextRange(start, end + 1);
    }

    static Pair<Boolean, String> detectFormatOfLine(String line) {
        String res = detectLog4jFormatOfLine(line);

        if (res == null || res.equals(UNKNOWN_FORMAT)) {
            String nginx = detectNginxFormatOfLine(line);
            if (nginx != null) {
                return Pair.of(false, nginx);
            }
        }

        if (res == null)
            return null;

        return Pair.of(true, res);
    }

    static String detectNginxFormatOfLine(String line) {
        Matcher matcher = NGINX_PATTERN.matcher(line);
        if (matcher.lookingAt()) {
            return "$remote_addr - $remote_user [$time_local] $any";
        }

        return null;
    }

    private static void appendMsIfPresent(StringBuilder sb, Matcher matcher) {
        String ms = matcher.group("ms");
        if (ms == null)
            return;

        String msSeparator = matcher.group("msSeparator");
        if (msSeparator != null)
            sb.append(msSeparator);

        for (int i = 0; i < ms.length(); i++) {
            sb.append('S');
        }
    }

    private static void appendTimeZoneIfPresent(StringBuilder sb, Matcher matcher) {
        if (matcher.group("timezone") == null)
            return;

        String separator = matcher.group("tzSeparator");
        if (separator != null)
            sb.append(separator);

        sb.append('z');
    }

    static String detectLog4jFormatOfLine(String line) {
        if (SPRING_PATTERN.matcher(line).matches())
            return SPRING_LOG4J_PATTERN;

        String dateField;
        TextRange datePos;

        Matcher matcher = DATE_ISO8601.matcher(line); // 2020-05-29 18:50:12,333
        if (matcher.find()) {
            StringBuilder sb = new StringBuilder();
            String dateSeparator = matcher.group(1);
            
            sb.append("%d{yyyy").append(dateSeparator).append("MM").append(dateSeparator).append("dd");

            String timeSeparator = matcher.group("timeSep");
            if (timeSeparator.equals("T"))
                timeSeparator = "'T'";
            sb.append(timeSeparator).append("HH:mm:ss");

            appendMsIfPresent(sb, matcher);
            appendTimeZoneIfPresent(sb, matcher);

            sb.append('}');

            dateField = sb.toString();
        } else {
            matcher = DATE_COMPACT.matcher(line); // 20200529 185012
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                sb.append("%d{yyyyMMdd");
                String separator = matcher.group(1);
                if (separator.equals("T")) {
                    sb.append("'T'");
                } else {
                    sb.append(separator);
                }
                sb.append("HHmmss");

                appendMsIfPresent(sb, matcher);
                appendTimeZoneIfPresent(sb, matcher);

                sb.append('}');

                dateField = sb.toString();
            } else {
                matcher = DATE_LONG.matcher(line); // 2020 Jul 21 15:04:01
                if (matcher.find()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("%d{dd MMM yyyy HH:mm:ss");

                    appendMsIfPresent(sb, matcher);
                    appendTimeZoneIfPresent(sb, matcher);

                    sb.append('}');
                    dateField = sb.toString();
                } else {
                    matcher = DATE_LONG_2.matcher(line); // 2020-Jul-21 15:04:01
                    if (matcher.find()) {
                        String dateSep = matcher.group("dateSep");
                        String dtSep = matcher.group("dtSep");

                        StringBuilder sb = new StringBuilder();
                        sb.append("%d{yyyy").append(dateSep).append("MMM").append(dateSep).append("dd").append(dtSep).append("HH:mm:ss");

                        appendMsIfPresent(sb, matcher);
                        appendTimeZoneIfPresent(sb, matcher);

                        sb.append('}');

                        dateField = sb.toString();
                    } else {
                        if (TIME_WITHOUT_DATE.matcher(line).find() || LEVEL.matcher(line).find()) {
                            return UNKNOWN_FORMAT;
                        }

                        return null;
                    }
                }
            }
        }

        datePos = new TextRange(matcher.start(), matcher.end());

        if (line.startsWith("[", datePos.getStart() - 1) && line.startsWith("]", datePos.getEnd())) {
            dateField = '[' + dateField + ']';
            datePos = new TextRange(datePos.getStart() - 1, datePos.getEnd() + 1);
        }

        matcher = LEVEL.matcher(line);

        if (datePos.getStart() > 0) {
            // expected format like "%p %d"
            matcher.region(0, datePos.getStart());

            if (!matcher.find())
                return UNKNOWN_FORMAT;

            TextRange levelPos = new TextRange(matcher.start(), matcher.end());
            String level = "%level";
            TextRange ra = expandRange(line, levelPos);
            if (!ra.equals(levelPos)) {
                level = '[' + level + ']';
                levelPos = ra;
            }

            if (levelPos.getStart() != 0)
                return UNKNOWN_FORMAT;

            String separator = getSeparator(line, levelPos.getEnd(), datePos.getStart());
            if (separator == null)
                return UNKNOWN_FORMAT;

            String messageSeparator = getMessageSeparator(line, datePos.getEnd());
            if (messageSeparator == null)
                return UNKNOWN_FORMAT;

            return level + separator + dateField + messageSeparator + "%m%n";
        }

        // expected format "%d %p" or "%d [%t] %p"
        matcher.region(datePos.getEnd(), line.length());

        if (matcher.find()) {
            TextRange levelPos = new TextRange(matcher.start(), matcher.end());
            String level = "%level";
            TextRange ra = expandRange(line, levelPos);
            if (!ra.equals(levelPos)) {
                level = '[' + level + ']';
                levelPos = ra;
            }

            String messageSeparator = getMessageSeparator(line, levelPos.getEnd());
            if (messageSeparator == null)
                return UNKNOWN_FORMAT;

            String separator = getSeparator(line, datePos.getEnd(), levelPos.getStart());
            if (separator != null) {
                return dateField + separator + level + messageSeparator + "%m%n";
            }

            matcher = THREAD_ITEM.matcher(line);
            matcher.region(datePos.getEnd(), levelPos.getStart());
            if (!matcher.matches())
                return UNKNOWN_FORMAT;

            String separator1 = getSeparator(line, datePos.getEnd(), matcher.start(1));
            if (separator1 == null)
                return UNKNOWN_FORMAT;
            String separator2 = getSeparator(line, matcher.end(1), levelPos.getStart());
            if (separator2 == null)
                return UNKNOWN_FORMAT;

            return dateField + separator1 + "[%t]" + separator2 + level + messageSeparator + "%m%n";
        }

        // Expected format "%d %msg%n" or "%d: %msg%n"
        String messageSeparator = getMessageSeparator(line, datePos.getEnd());
        if (messageSeparator == null) {
            if (line.startsWith(": ", datePos.getEnd())) {
                messageSeparator = ": ";
            } else {
                return UNKNOWN_FORMAT;
            }
        }

        return dateField + messageSeparator + "%m%n";
    }

    /**
     * @return " " or "" or {@code null}
     */
    @Nullable
    private static String getMessageSeparator(String line, int lastFieldEnd) {
        if (line.startsWith(" ", lastFieldEnd))
            return  " ";

        if (line.startsWith("[", lastFieldEnd))
            return "";

        return null;
    }

    @Nullable
    private static String getSeparator(String line, int start, int end) {
        if (start == end)
            return "";

        if (start + 1 == end) {
            char a = line.charAt(start);
            if (a == ' ')
                return " ";

            return null;
        }

        assert start < end;

        for (int i = start; i < end; i++) {
            if (line.charAt(i) != ' ')
                return null;
        }

        if (line.startsWith("[", end) && start > 0 && line.charAt(start - 1) == ']')
            return null;

        return " ";
    }

}
