package com.xuvigan.aurumbuyer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/** Pure cost math of shop and booster entries. */
class CostMathTest {

    @Test
    void shopCostGrowsGeometrically() {
        ShopConfig.ShopItem item = new ShopConfig.ShopItem(
                "golden_apple", null, 8, 10, "display", Collections.<String>emptyList(),
                500.0D, 10.0D, -1, 5, "", 0, false, Collections.<String>emptyList(), "", "", "");
        assertEquals(500.0D, item.cost(0), 0.0001D);
        assertEquals(550.0D, item.cost(1), 0.0001D);
        assertEquals(605.0D, item.cost(2), 0.0001D);
        assertEquals(500.0D, item.cost(-3), 0.0001D);
    }

    @Test
    void boosterCostAndPercent() {
        BoosterConfig.Category category = new BoosterConfig.Category(
                "ores", "Ruda", null, 11, 20, 1000.0D, 15.0D, 5.0D,
                Collections.<String>emptyList(), 0);
        assertEquals(1000.0D, category.cost(0), 0.0001D);
        assertEquals(1150.0D, category.cost(1), 0.0001D);
        assertEquals(0.0D, category.percentAt(-1), 0.0001D);
        assertEquals(15.0D, category.percentAt(3), 0.0001D);
        assertEquals(100.0D, category.percentAt(999), 0.0001D);
    }

    @Test
    void tierMathUsesHighestReachedTier() {
        // mirrors MultiplierService: per-step-percent * index of the reached tier
        double[] tiers = {0.0D, 100000.0D, 500000.0D, 2000000.0D};
        assertEquals(-1, index(tiers, -1.0D));
        assertEquals(0, index(tiers, 0.0D));
        assertEquals(1, index(tiers, 150000.0D));
        assertEquals(3, index(tiers, 2500000.0D));
    }

    private static int index(double[] tiers, double total) {
        int index = -1;
        for (int i = 0; i < tiers.length; i++) {
            if (total + 0.0000001D >= tiers[i]) {
                index = i;
            }
        }
        return index;
    }
}
