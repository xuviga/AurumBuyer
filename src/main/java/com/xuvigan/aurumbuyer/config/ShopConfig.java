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

/** shop.yml: aurum shop items with a growing per-player price. */
public final class ShopConfig {

    /** One purchasable shop entry. */
    public static final class ShopItem {

        private final String id;
        private final Material material;
        private final int amount;
        private final int slot;
        private final String display;
        private final List<String> lore;
        private final double baseCost;
        private final double increasePercent;
        private final int globalLimit;
        private final int personalLimit;
        private final String permission;
        private final int customModelData;
        private final boolean glow;
        private final List<String> commands;
        private final String successMessage;
        private final String limitMessage;
        private final String noPermissionMessage;

        public ShopItem(String id, Material material, int amount, int slot, String display, List<String> lore,
                        double baseCost, double increasePercent, int globalLimit, int personalLimit,
                        String permission, int customModelData, boolean glow, List<String> commands,
                        String successMessage, String limitMessage, String noPermissionMessage) {
            this.id = id;
            this.material = material;
            this.amount = amount;
            this.slot = slot;
            this.display = display;
            this.lore = lore;
            this.baseCost = baseCost;
            this.increasePercent = increasePercent;
            this.globalLimit = globalLimit;
            this.personalLimit = personalLimit;
            this.permission = permission == null ? "" : permission.trim();
            this.customModelData = customModelData;
            this.glow = glow;
            this.commands = commands;
            this.successMessage = successMessage;
            this.limitMessage = limitMessage;
            this.noPermissionMessage = noPermissionMessage;
        }

        public String id() {
            return id;
        }

        public Material material() {
            return material;
        }

        public int amount() {
            return amount;
        }

        public int slot() {
            return slot;
        }

        public String display() {
            return display;
        }

        public List<String> lore() {
            return lore;
        }

        public double baseCost() {
            return baseCost;
        }

        public double increasePercent() {
            return increasePercent;
        }

        public int globalLimit() {
            return globalLimit;
        }

        public int personalLimit() {
            return personalLimit;
        }

        public String permission() {
            return permission;
        }

        public boolean hasPermissionNode() {
            return !permission.isEmpty();
        }

        public int customModelData() {
            return customModelData;
        }

        public boolean glow() {
            return glow;
        }

        public List<String> commands() {
            return commands;
        }

        public String successMessage() {
            return successMessage;
        }

        public String limitMessage() {
            return limitMessage;
        }

        public String noPermissionMessage() {
            return noPermissionMessage;
        }

        /** Cost of the next purchase for a player who already bought {@code bought} times. */
        public double cost(int bought) {
            int safe = bought < 0 ? 0 : bought;
            return baseCost * Math.pow(1.0D + increasePercent / 100.0D, safe);
        }
    }

    private final Map<String, ShopItem> items = new LinkedHashMap<String, ShopItem>();
    private final boolean enabled;
    private final double defaultIncreasePercent;
    private final int maxPurchasesPerClick;
    private final boolean requirePermission;

    public ShopConfig(FileConfiguration cfg, ConfigManager config) {
        this.enabled = config == null ? true : config.shopEnabled();
        this.defaultIncreasePercent = config == null ? 8.0D : config.shopPriceIncreasePercent();
        this.maxPurchasesPerClick = config == null ? 1 : config.shopMaxPurchasesPerClick();
        this.requirePermission = config == null ? false : config.shopRequirePermission();
        if (cfg == null) {
            return;
        }
        ConfigurationSection section = cfg.getConfigurationSection("items");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            items.put(key.toLowerCase(Locale.ENGLISH), read(key.toLowerCase(Locale.ENGLISH), itemSection));
        }
    }

    private ShopItem read(String id, ConfigurationSection section) {
        Material material = Material.matchMaterial(String.valueOf(section.getString("material", "STONE")).toUpperCase(Locale.ENGLISH));
        if (material == null) {
            material = Material.STONE;
        }
        int amount = Math.max(1, section.getInt("amount", 1));
        int slot = section.getInt("slot", 0);
        String display = Text.color(section.getString("display", id));
        List<String> lore = Text.color(section.getStringList("lore"));
        double baseCost = section.getDouble("base-cost", 0.0D);
        double increase = section.getDouble("price-increase-percent", defaultIncreasePercent);
        int globalLimit = section.getInt("global-limit", -1);
        int personalLimit = section.getInt("personal-limit", -1);
        String permission = section.getString("permission", "");
        int modelData = section.getInt("custom-model-data", 0);
        boolean glow = section.getBoolean("glow", false);
        List<String> commands = new ArrayList<String>(section.getStringList("commands"));
        ConfigurationSection messagesSection = section.getConfigurationSection("messages");
        String success = Text.color(messagesSection == null ? "" : messagesSection.getString("success", ""));
        String limit = Text.color(messagesSection == null ? "" : messagesSection.getString("limit-reached", ""));
        String noPermission = Text.color(messagesSection == null ? "" : messagesSection.getString("no-permission", ""));
        return new ShopItem(id, material, amount, slot, display, lore, baseCost, increase, globalLimit,
                personalLimit, permission, modelData, glow, commands, success, limit, noPermission);
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxPurchasesPerClick() {
        return maxPurchasesPerClick;
    }

    public boolean requirePermission() {
        return requirePermission;
    }

    public Collection<ShopItem> items() {
        return Collections.unmodifiableCollection(items.values());
    }

    public ShopItem item(String id) {
        return id == null ? null : items.get(id.toLowerCase(Locale.ENGLISH));
    }

    public ShopItem bySlot(int slot) {
        for (ShopItem item : items.values()) {
            if (item.slot() == slot) {
                return item;
            }
        }
        return null;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}