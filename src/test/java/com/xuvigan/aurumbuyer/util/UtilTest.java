package com.xuvigan.aurumbuyer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class UtilTest {

    @Test
    void numbersParseAndFormat() {
        assertEquals(1.5D, Numbers.parse("1,5", 0.0D), 0.0001D);
        assertEquals(7.0D, Numbers.parse("oops", 7.0D), 0.0001D);
        assertEquals("1,234.50", Numbers.format(1234.5D, "#,##0.00"));
        assertEquals("1,234.5", Numbers.round(1234.5D));
        assertEquals(42, Numbers.parseInt("42", 0));
        assertEquals(-1, Numbers.parseInt("x", -1));
    }

    @Test
    void timeFormatting() {
        assertEquals("00:59", TimeUtil.mmss(59L));
        assertEquals("01:01", TimeUtil.mmss(61L));
        assertEquals("1:01:01", TimeUtil.mmss(3661L));
        assertEquals("00:00", TimeUtil.mmss(-5L));
        assertEquals(600000L, TimeUtil.millisFromMinutes(10L));
        assertEquals(3600000L, TimeUtil.millisFromHours(1L));
    }

    @Test
    void colorsAndPairs() {
        String colored = Text.color("&aGreen");
        assertEquals('\u00A7', colored.charAt(0));
        String hex = Text.color("&f#FFAA00");
        assertTrue(hex.contains("\u00A7x"));
        assertFalse(hex.contains("#"));
        assertEquals("hello 5", Text.formatPairs("hello %n%", "n", Integer.valueOf(5)));
        assertEquals("a-b", Text.formatPairs("a-%x%", "x", "b"));
        assertEquals("", Text.color((String) null));
        assertEquals(2, Text.color(Arrays.asList("&a1", "&b2")).size());
    }
}
