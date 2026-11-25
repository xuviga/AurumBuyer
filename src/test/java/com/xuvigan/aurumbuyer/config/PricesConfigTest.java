package com.xuvigan.aurumbuyer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PricesConfigTest {

    @Test
    void explicitItemsAndWildcardGroups() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("items.DIAMOND.price", Double.valueOf(250.0D));
        yaml.set("items.DIAMOND.category", "ores");
        yaml.set("groups.*_LOG.price", Double.valueOf(12.0D));
        yaml.set("groups.*_LOG.category", "blocks");

        PricesConfig prices = new PricesConfig(yaml);

        assertEquals(250.0D, prices.getById("DIAMOND").price(), 0.0001D);
        assertEquals(250.0D, prices.getById("diamond").price(), 0.0001D);
        assertEquals("ores", prices.getById("DIAMOND").category());
        assertEquals(12.0D, prices.getById("OAK_LOG").price(), 0.0001D);
        assertEquals("blocks", prices.getById("BIRCH_LOG").category());
        assertNull(prices.getById("STONE"));
        assertNull(prices.getById(null));
        assertEquals(1, prices.size());
        assertFalse(prices.isEmpty());
    }

    @Test
    void emptyConfigurationIsEmpty() {
        PricesConfig prices = new PricesConfig(new YamlConfiguration());
        assertTrue(prices.isEmpty());
        assertEquals(0, prices.size());
        assertNull(prices.getById("DIAMOND"));
        assertTrue(prices.all().isEmpty());
    }
}
