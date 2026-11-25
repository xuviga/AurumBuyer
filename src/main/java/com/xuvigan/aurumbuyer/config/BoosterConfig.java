package com.xuvigan.aurumbuyer.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.xuvigan.aurumbuyer.util.Text;

/** boosters.yml: one line per item category. */
public final class BoosterConfig {

    /** One permanent booster line. */
    public static final class Category {

        private final String id;
        private final String display;
        private final Material material;
        private final int slot;
        private final int maxLevel;
        private final double baseCost;
        private final double growthPercent;
        private final double percentPerLevel;
        private final List<String> lore;
        private final int customModelData;

        public Category(String id, String display, Material material, int slot, int maxLevel, double baseCost,
                        double growthPercent, double percentPerLevel, List<String> lore, int customModelData) {
            this.id = id;
            this.display = display;
            this.material = material;
            this.slot = slot;
            this.maxLevel = maxLevel;
            this.baseCost = baseCost;
            this.growthPercent = growthPercent;
            this.percentPerLevel = percentPerLevel;
            this.lore = lore;
            this.customModelData = customModelData;
        }

        public String id() {
            return id;
        }

        public String display() {
            return display;
        }

        public Material material() {
            return material;
        }

        public int slot() {
            return slot;
        }

        public int maxLevel() {
            return maxLevel;
        }

        public double baseCost() {
            return baseCost;
        }

        public double growthPercent() {
            return growthPercent;
        }

        public double percentPerLevel() {
            return percentPerLevel;
        }

        public List<String> lore() {
            return lore;
        }

        public int customModelData() {
            return customModelData;
        }

        /** Cost of reaching {@code level + 1}. */
        public double cost(int level) {
            int safe = level < 0 ? 0 : level;
            return baseCost * Math.pow(1.0D + growthPercent / 100.0D, safe);
        }

        public double percentAt(int level) {
            int safe = level < 0 ? 0 : Math.min(level, maxLevel);
            return percentPerLevel * safe;
        }
    }

    private final Map<String, Category> categories = new LinkedHashMap<String, Category>();

    public BoosterConfig(FileConfiguration cfg) {
        if (cfg == null) {
            return;
        }
        ConfigurationSection section = cfg.getConfigurationSection("categories");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection category = section.getConfigurationSection(key);
            if (category == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ENGLISH);
            Material material = Material.matchMaterial(String.valueOf(category.getString("material", "STONE")).toUpperCase(Locale.ENGLISH));
            if (material == null) {
                material = Material.STONE;
            }
            categories.put(id, new Category(id,
                    Text.color(category.getString("display", id)),
                    material,
                    category.getInt("slot", 0),
                    Math.max(1, category.getInt("max-level", 20)),
                    category.getDouble("base-cost", 1000.0D),
                    category.getDouble("cost-growth-percent", 15.0D),
                    category.getDouble("percent-per-level", 5.0D),
                    Text.color(category.getStringList("lore")),
                    category.getInt("custom-model-data", 0)));
        }
    }

    public Collection<Category> categories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    public List<String> ids() {
        return new ArrayList<String>(categories.keySet());
    }

    public Category category(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ENGLISH));
    }

    public int maxLevel(String id) {
        Category category = category(id);
        return category == null ? 0 : category.maxLevel();
    }

    public double baseCost(String id) {
        Category category = category(id);
        return category == null ? 0.0D : category.baseCost();
    }

    public double percentPerLevel(String id) {
        Category category = category(id);
        return category == null ? 0.0D : category.percentPerLevel();
    }

    public double growthPercent(String id) {
        Category category = category(id);
        return category == null ? 0.0D : category.growthPercent();
    }

    public double cost(String id, int level) {
        Category category = category(id);
        return category == null ? 0.0D : category.cost(level);
    }

    public int size() {
        return categories.size();
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }
}