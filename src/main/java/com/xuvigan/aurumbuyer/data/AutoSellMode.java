package com.xuvigan.aurumbuyer.data;

/** Auto-sell mode of a player profile. */
public enum AutoSellMode {

    ENABLED,
    UNTIL_QUIT,
    DISABLED;

    public static AutoSellMode parse(String value) {
        if (value == null) {
            return DISABLED;
        }
        String key = value.trim().toUpperCase(java.util.Locale.ENGLISH);
        if ("ENABLED".equals(key) || "ON".equals(key) || "TRUE".equals(key)) {
            return ENABLED;
        }
        if ("UNTIL_QUIT".equals(key) || "UNTILQUIT".equals(key)) {
            return UNTIL_QUIT;
        }
        return DISABLED;
    }

    public AutoSellMode next() {
        if (this == ENABLED) {
            return UNTIL_QUIT;
        }
        if (this == UNTIL_QUIT) {
            return DISABLED;
        }
        return ENABLED;
    }

    public boolean active() {
        return this != DISABLED;
    }
}