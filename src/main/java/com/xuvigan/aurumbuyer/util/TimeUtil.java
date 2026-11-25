package com.xuvigan.aurumbuyer.util;

/** Time formatting helpers. Digit-only formats, no localised words. */
public final class TimeUtil {

    private TimeUtil() {
    }

    /** MM:SS, extended to H:MM:SS when the value reaches one hour. */
    public static String mmss(long seconds) {
        long value = seconds < 0L ? 0L : seconds;
        long hours = value / 3600L;
        long minutes = (value % 3600L) / 60L;
        long secs = value % 60L;
        if (hours > 0L) {
            return hours + ":" + two(minutes) + ":" + two(secs);
        }
        return two(minutes) + ":" + two(secs);
    }

    private static String two(long value) {
        return value < 10L ? "0" + value : String.valueOf(value);
    }

    public static long millisFromMinutes(long minutes) {
        return minutes * 60000L;
    }

    public static long millisFromHours(long hours) {
        return hours * 3600000L;
    }

    public static String clock(long timestampMillis) {
        long seconds = timestampMillis / 1000L;
        long hours = (seconds / 3600L) % 24L;
        long minutes = (seconds / 60L) % 60L;
        return two(hours) + ":" + two(minutes);
    }
}