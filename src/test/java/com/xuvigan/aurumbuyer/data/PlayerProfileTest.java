package com.xuvigan.aurumbuyer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerProfileTest {

    private PlayerProfile profile() {
        return new PlayerProfile(UUID.randomUUID(), "Tester", 2);
    }

    @Test
    void pointsAndTotals() {
        PlayerProfile profile = profile();
        profile.addPoints(10.0D);
        assertEquals(10.0D, profile.points(), 0.0001D);
        assertEquals(10.0D, profile.pointsTotal(), 0.0001D);
        assertTrue(profile.takePoints(4.0D));
        assertEquals(6.0D, profile.points(), 0.0001D);
        assertEquals(10.0D, profile.pointsTotal(), 0.0001D);
        assertFalse(profile.takePoints(100.0D));
        assertEquals(6.0D, profile.points(), 0.0001D);
    }

    @Test
    void ultaPointsAreClamped() {
        PlayerProfile profile = profile();
        profile.addUltaPoints(50.0D, 40.0D);
        assertEquals(40.0D, profile.ultaPoints(), 0.0001D);
        assertTrue(profile.takeUltaPoints(10.0D));
        assertEquals(30.0D, profile.ultaPoints(), 0.0001D);
        assertFalse(profile.takeUltaPoints(100.0D));
    }

    @Test
    void personalLimitsResetWhenDue() {
        PlayerProfile profile = profile();
        profile.setPersonalLimit("DIAMOND", 100L);
        profile.personalResetAt(System.currentTimeMillis() - 1000L);
        profile.resetPersonalLimitsIfDue(24L);
        assertEquals(0L, profile.personalLimitConsumed("DIAMOND"));
        assertTrue(profile.personalResetAt() > System.currentTimeMillis());
    }

    @Test
    void historyIsCappedAndNewestFirst() {
        PlayerProfile profile = profile();
        profile.addHistory(new HistoryEntry(1L, "STONE", 1, 1.0D, 0.1D));
        profile.addHistory(new HistoryEntry(2L, "DIAMOND", 2, 2.0D, 0.2D));
        profile.addHistory(new HistoryEntry(3L, "GOLD_INGOT", 3, 3.0D, 0.3D));
        assertEquals(2, profile.history().size());
        assertEquals("GOLD_INGOT", profile.history().get(0).itemId());
        assertEquals("DIAMOND", profile.history().get(1).itemId());
    }

    @Test
    void soldCountersAreNormalised() {
        PlayerProfile profile = profile();
        profile.addSold("diamond", 5L);
        assertEquals(5L, profile.sold("DIAMOND"));
        profile.addSold("DIAMOND", -5L);
        assertEquals(0L, profile.sold("DIAMOND"));
        assertTrue(profile.soldMap().isEmpty());
    }
}
