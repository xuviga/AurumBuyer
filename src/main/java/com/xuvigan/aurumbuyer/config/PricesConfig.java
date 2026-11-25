package com.xuvigan.aurumbuyer.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** prices.yml: explicit item entries plus wildcard groups. */
public final class PricesConfig {

    /** Resolved buy-back worth of a single item id. */
    public static final class ItemWorth {

        private final String id;
        private final double price;
        private final String category;
        private final int customModelData;
        private final boolean wildcard;
        private final boolean globalEnabled;
        private final long globalLimit;
        private final double globalMultiplier;
        private final boolean personalEnabled;
        private final long personalLimit;
        private final double personalMultiplier;

        public ItemWorth(String id, double price, String category, int customModelData, boolean wildcard,
                         boolean globalEnabled, long globalLimit, double globalMultiplier,
                         boolean personalEnabled, long personalLimit, double personalMultiplier) {
            this.id = id;
            this.price = price;
            this.category = category == null || category.trim().isEmpty() ? "misc" : category.trim();
            this.customModelData = customModelData;
            this.wildcard = wildcard;
            this.globalEnabled = globalEnabled;
            this.globalLimit = globalLimit;
            this.globalMultiplier = globalMultiplier;
            this.personalEnabled = personalEnabled;
            this.personalLimit = personalLimit;
            this.personalMultiplier = personalMultiplier;
        }

        public String id() {
            return id;
        }

        public double price() {
            return price;
        }

        public String category() {
            return category;
        }

        public int customModelData() {
            return customModelData;
        }

        public boolean wildcard() {
            return wildcard;
        }

        public boolean limited() {
            return globalEnabled || personalEnabled;
        }

        public boolean globalEnabled() {
            return globalEnabled;
        }

        public long globalLimit() {
            return globalLimit;
        }

        public double globalMultiplier() {
            return globalMultiplier;
        }

        public boolean personalEnabled() {
            return personalEnabled;
        }

        public long personalLimit() {
            return personalLimit;
        }

        public double personalMultiplier() {
            return personalMultiplier;
        }

        public String getId() {
            return id;
        }

        public double getPrice() {
            return price;
        }

        public String getCategory() {
            return category;
        }
    }

    private static final class Group {

        private final String key;
        private final Pattern pattern;
        private final ItemWorth worth;

        private Group(String key, ItemWorth worth) {
            this.key = key;
            this.pattern = Pattern.compile(wildcardRegex(key));
            this.worth = worth;
        }

        private boolean matches(String id) {
            return pattern.matcher(id).matches();
        }
    }

    private final Map<String, ItemWorth> items = new LinkedHashMap<String, ItemWorth>();
    private final List<Group> groups = new ArrayList<Group>();

    public PricesConfig(FileConfiguration cfg) {
        if (cfg == null) {
            return;
        }
        ConfigurationSection itemsSection = cfg.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ItemWorth worth = read(normalize(key), itemsSection.getConfigurationSection(key), false);
                if (worth != null) {
                    items.put(normalize(key), worth);
                }
            }
        }
        ConfigurationSection groupsSection = cfg.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                ItemWorth worth = read(normalize(key), groupsSection.getConfigurationSection(key), true);
                if (worth != null) {
                    groups.add(new Group(normalize(key), worth));
                }
            }
        }
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ENGLISH);
    }

    private static ItemWorth read(String id, ConfigurationSection section, boolean wildcard) {
        if (section == null) {
            return null;
        }
        double price = section.getDouble("price", 0.0D);
        String category = section.getString("category", "misc");
        int modelData = section.getInt("model-data", section.getInt("custom-model-data", 0));
        ConfigurationSection limited = section.getConfigurationSection("limited");
        ConfigurationSection global = limited == null ? null : limited.getConfigurationSection("global");
        ConfigurationSection personal = limited == null ? null : limited.getConfigurationSection("personal");
        boolean globalEnabled = global != null && global.getBoolean("enabled", false);
        long globalLimit = global == null ? 0L : global.getLong("limit", 0L);
        double globalMultiplier = global == null ? 1.0D : global.getDouble("multiplier", 1.0D);
        boolean personalEnabled = personal != null && personal.getBoolean("enabled", false);
        long personalLimit = personal == null ? 0L : personal.getLong("limit", 0L);
        double personalMultiplier = personal == null ? 1.0D : personal.getDouble("multiplier", 1.0D);
        return new ItemWorth(id, price, category, modelData, wildcard,
                globalEnabled, globalLimit, globalMultiplier,
                personalEnabled, personalLimit, personalMultiplier);
    }

    private static String wildcardRegex(String raw) {
        String key = normalize(raw);
        StringBuilder out = new StringBuilder(key.length() + 8);
        int start = 0;
        while (true) {
            int star = key.indexOf('*', start);
            if (star < 0) {
                out.append(Pattern.quote(key.substring(start)));
                break;
            }
            out.append(Pattern.quote(key.substring(start, star)));
            out.append(".*");
            start = star + 1;
        }
        return out.toString();
    }

    public ItemWorth get(Material material) {
        return material == null ? null : getById(material.name());
    }

    public ItemWorth getById(String id) {
        if (id == null) {
            return null;
        }
        String key = normalize(id);
        ItemWorth direct = items.get(key);
        if (direct != null) {
            return direct;
        }
        for (Group group : groups) {
            if (group.matches(key)) {
                return group.worth;
            }
        }
        return null;
    }

    public ItemWorth resolve(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        Material type = stack.getType();
        if (type == null || type == Material.AIR) {
            return null;
        }
        ItemWorth worth = get(type);
        if (worth == null || worth.customModelData() <= 0) {
            return worth;
        }
        try {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == worth.customModelData()) {
                return worth;
            }
        } catch (Throwable ignored) {
            // older server cannot read custom model data - keep the entry usable
            return worth;
        }
        // the entry targets a specific custom model: this stack is a different item
        return null;
    }

    public Collection<ItemWorth> all() {
        return Collections.unmodifiableCollection(items.values());
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty() && groups.isEmpty();
    }
}