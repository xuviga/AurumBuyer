package com.xuvigan.aurumbuyer.booster;

import java.util.List;

/** One booster line exposed to the GUI, commands and placeholders. */
public final class BoosterCategory {

    private final String id;
    private final String display;
    private final String materialName;
    private final int slot;
    private final int maxLevel;
    private final double baseCost;
    private final double growthPercent;
    private final double percentPerLevel;
    private final List<String> lore;
    private final int customModelData;

    public BoosterCategory(String id, String display, String materialName, int slot, int maxLevel,
                           double baseCost, double growthPercent, double percentPerLevel,
                           List<String> lore, int customModelData) {
        this.id = id == null ? "" : id;
        this.display = display == null ? "" : display;
        this.materialName = materialName == null ? "STONE" : materialName;
        this.slot = slot;
        this.maxLevel = Math.max(1, maxLevel);
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

    public String materialName() {
        return materialName;
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

    /** Cost of the level {@code level} (0 = buy the first level). */
    public double cost(int level) {
        int safe = level < 0 ? 0 : level;
        return baseCost * Math.pow(1.0D + growthPercent / 100.0D, safe);
    }

    /** Permanent percent granted by {@code level}. */
    public double percentAt(int level) {
        int safe = level < 0 ? 0 : Math.min(level, maxLevel);
        return percentPerLevel * safe;
    }

    @Override
    public String toString() {
        return "BoosterCategory{" + id + "}";
    }
}
