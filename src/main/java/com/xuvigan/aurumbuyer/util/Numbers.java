package com.xuvigan.aurumbuyer.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Number formatting helpers (thread safe: a fresh formatter per call). */
public final class Numbers {

    public static final String DEFAULT_PATTERN = "#,##0.00";

    private Numbers() {
    }

    public static String format(double value) {
        return format(value, DEFAULT_PATTERN);
    }

    public static String format(double value, String pattern) {
        String safe = pattern == null || pattern.trim().isEmpty() ? DEFAULT_PATTERN : pattern.trim();
        DecimalFormat formatter;
        try {
            formatter = new DecimalFormat(safe, DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException ex) {
            formatter = new DecimalFormat(DEFAULT_PATTERN, DecimalFormatSymbols.getInstance(Locale.US));
        }
        return formatter.format(value);
    }

    public static String round(double value) {
        return format(value, "#,##0.##");
    }

    public static double parse(String input, double fallback) {
        if (input == null) {
            return fallback;
        }
        String cleaned = input.trim().replace(",", ".");
        if (cleaned.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static long parseLong(String input, long fallback) {
        if (input == null) {
            return fallback;
        }
        String cleaned = input.trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static int parseInt(String input, int fallback) {
        return (int) parseLong(input, fallback);
    }
}