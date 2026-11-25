package com.xuvigan.aurumbuyer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;

/**
 * Colour helper: legacy '&' codes plus #RRGGBB hex support (1.16+ rendering,
 * implemented as the classic section-x escape sequence so it also works on
 * legacy clients).
 */
public final class Text {

    private static final char SECTION = '\u00A7';

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return hex(ChatColor.translateAlternateColorCodes('&', input));
    }

    private static String hex(String input) {
        int first = input.indexOf('#');
        if (first < 0) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '#' && i + 6 < input.length() && isHex(input, i + 1)) {
                out.append(SECTION).append('x');
                for (int k = 0; k < 6; k++) {
                    out.append(SECTION).append(input.charAt(i + 1 + k));
                }
                i += 7;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static boolean isHex(String value, int from) {
        for (int i = from; i < from + 6; i++) {
            char c = value.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static String replace(String input, String key, String value) {
        if (input == null) {
            return "";
        }
        if (key == null || key.isEmpty()) {
            return input;
        }
        String token = key.indexOf('%') >= 0 ? key : "%" + key + "%";
        String replacement = value == null ? "" : value;
        return input.replace(token, replacement);
    }

    public static String format(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }
        String out = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getKey() != null) {
                out = replace(out, entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    public static String formatPairs(String input, Object... pairs) {
        if (input == null) {
            return "";
        }
        if (pairs == null || pairs.length == 0) {
            return input;
        }
        String out = input;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i] == null) {
                continue;
            }
            out = replace(out, String.valueOf(pairs[i]), pairs[i + 1] == null ? "" : String.valueOf(pairs[i + 1]));
        }
        return out;
    }

    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<String>();
        if (input == null) {
            return out;
        }
        for (String line : input) {
            out.add(color(line));
        }
        return out;
    }
}