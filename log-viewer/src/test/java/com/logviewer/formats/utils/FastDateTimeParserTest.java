package com.logviewer.formats.utils;

import com.logviewer.utils.LvDateUtils;
import org.junit.Test;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class FastDateTimeParserTest {

    private static final ZoneId[] OFFSETS = new ZoneId[] {ZoneId.of("UTC"), ZoneId.of("Europe/Paris"), ZoneId.of("Europe/Moscow"),
            ZoneId.of("America/Montreal") };

    private static final Instant[] DATES = new Instant[]{
            Instant.parse("2011-12-03T10:15:30Z"),
            Instant.parse("2011-12-03T10:15:30Z"),
            Instant.parse("2021-01-01T00:15:30Z"),
            Instant.parse("2000-12-31T02:59:59.111Z")
    };

    private static final String[] FORMATS = {
            "yyyy-MM-dd HH:mm:ss.SSS","yyyy-MM-dd HH:mm:ss.SSSS", "yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS",
            "yyyy-MMM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss.SSS z", "yyyy-MM-dd HH:mm:ss.SSS zz", "yyyy-MM-dd HH:mm:ss.SSS zzz",
            "yyyy-MM-dd HH:mm:ss.SSS Z", "yyyy-MM-dd HH:mm:ss.SSS ZZ", "yyyy-MM-dd HH:mm:ss.SSS ZZZ", "yyyy-MM-dd HH:mm:ss.SSS ZZZZ","yyyy-MM-dd HH:mm:ss.SSS ZZZZZ",

            "yyyy-MM-dd HH:mm:ss.SSS X", "yyyy-MM-dd HH:mm:ss.SSS XX", "yyyy-MM-dd HH:mm:ss.SSS XXX", "yyyy-MM-dd HH:mm:ss.SSS XXXX",
            "yyyy-MM-dd HH:mm:ss.SSS XXXXX",

            "yyyyMMddHHmmssSSSz", "yyyyMMddHHmmssSSSZ", "yyyyMMddHHmmssSSSX",
    };

    @Test
    public void testBounds() throws ParseException {
        notParsed("yyyy-MM-dd HH:mm:ss.SSS Z", "2020-04-11 10:00:03.555 +00009999");
        check("yyyy-MM-dd HH:mm:ss.SSS Z", "2020-04-11 10:00:03.555 +0000", Instant.parse("2020-04-11T10:00:03.555Z"));
        check("yyyy-MM-dd HH:mm:ss.SSS Z", "2020-04-11 10:00:03.555 +0000.", Instant.parse("2020-04-11T10:00:03.555Z"));

        check("yyyy-MM-dd HH:mm:ss.SSS z", "2020-04-11 10:00:03.555 GMT", Instant.parse("2020-04-11T10:00:03.555Z"));
        check("yyyy-MM-dd HH:mm:ss.SSS z", "2020-04-11 10:00:03.555 GMT.", Instant.parse("2020-04-11T10:00:03.555Z"));
        notParsed("yyyy-MM-dd HH:mm:ss.SSS z", "2020-04-11 10:00:03.555 GMTTTT");
    }

    private void notParsed(String pattern, String text) {
        BiFunction<String, ParsePosition, Supplier<Instant>> formatter = FastDateTimeParser.createFormatter(pattern, null);

        ParsePosition position = new ParsePosition(0);
        Supplier<Instant> res = formatter.apply(text, position);

        assertNull(res);
    }

    private void check(String pattern, String text, Instant expectedResult) {
        BiFunction<String, ParsePosition, Supplier<Instant>> formatter = FastDateTimeParser.createFormatter(pattern, null);

        ParsePosition position = new ParsePosition(0);
        Instant res = formatter.apply(text, position).get();

        assertTrue(text.substring(position.getIndex()).matches("\\.*"));

        assertEquals(expectedResult, res);
    }

    @Test
    public void testDefaultDate() throws ParseException {
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";

        SimpleDateFormat format = new SimpleDateFormat(pattern);
        BiFunction<String, ParsePosition, Supplier<Instant>> formatter = FastDateTimeParser.createFormatter(pattern, null);

        String text = "2020-04-11 10:00:03.111";
        ParsePosition position = new ParsePosition(0);
        Instant res = formatter.apply(text, position).get();

        assertEquals(text.length(), position.getIndex());

        assertEquals(LvDateUtils.toNanos(format.parse(text)), LvDateUtils.toNanos(res));
    }

    @Test
    public void testCorrectParsing() {
        for (Instant date : DATES) {
            for (ZoneId offset : OFFSETS) {
                for (String format : FORMATS) {
                    doTest(date.atZone(offset), format);
                }
            }
        }
    }

    private void doTest(ZonedDateTime date, String pattern) {
        try {
            String str = DateTimeFormatter.ofPattern(pattern).format(date);

            BiFunction<String, ParsePosition, Supplier<Instant>> formatter = FastDateTimeParser.createFormatter(pattern, TimeZone.getTimeZone(date.getZone()));

            ParsePosition position = new ParsePosition(0);
            Supplier<Instant> res = formatter.apply(str, position);
            assertNotNull(res);

            assertEquals(str.length(), position.getIndex());

            assertEquals(date.toInstant(), res.get());

            // Non default position
            position.setIndex(3);
            res = formatter.apply("..." + str + "...", position);
            assertNotNull(res);
            assertEquals(str.length() + 3, position.getIndex());
            assertEquals(date.toInstant(), res.get());

            // no IndexOutOfBoundException when not enough length
            if (!str.matches(".+GMT[+-]\\d\\d:?\\d\\d"))
            for (int i = 3; i < str.length() - 5; i++) {
                position.setIndex(0);
                String substring = str.substring(0, i);
                res = formatter.apply(substring, new ParsePosition(0));
                assertNull(substring, res);
            }
        } catch (Throwable e) {
            throw new RuntimeException(date.toString() + ", " + pattern, e);
        }
    }
}