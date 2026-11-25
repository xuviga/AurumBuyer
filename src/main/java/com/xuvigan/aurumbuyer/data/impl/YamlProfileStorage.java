package com.xuvigan.aurumbuyer.data.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.HistoryEntry;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.data.ProfileManager;

/**
 * YAML backed profile storage: playerdata/&lt;uuid&gt;.yml, in-memory cache,
 * asynchronous writes and a lightweight bulk reader for leaderboards.
 */
public final class YamlProfileStorage implements ProfileManager {

    /** Minimal record used by top lists (no full profile load). */
    public static final class LightProfile {

        private final UUID uuid;
        private final String name;
        private final double moneyTotal;
        private final double pointsTotal;

        public LightProfile(UUID uuid, String name, double moneyTotal, double pointsTotal) {
            this.uuid = uuid;
            this.name = name == null ? "" : name;
            this.moneyTotal = moneyTotal;
            this.pointsTotal = pointsTotal;
        }

        public UUID uuid() {
            return uuid;
        }

        public String name() {
            return name;
        }

        public double moneyTotal() {
            return moneyTotal;
        }

        public double pointsTotal() {
            return pointsTotal;
        }
    }

    private final Plugin plugin;
    private final File directory;
    private final int historySize;
    private final double startBalance;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<UUID, PlayerProfile>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<String, UUID>();

    private long personalResetHours = 24L;

    public YamlProfileStorage(Plugin plugin) {
        this(plugin, new File(plugin.getDataFolder(), "playerdata"), 100, 0.0D);
    }

    public YamlProfileStorage(Plugin plugin, File directory, int historySize, double startBalance) {
        this.plugin = plugin;
        this.directory = directory;
        this.historySize = historySize <= 0 ? 100 : historySize;
        this.startBalance = startBalance;
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("[AurumBuyer] Could not create " + directory.getAbsolutePath());
        }
    }

    public void setPersonalResetHours(long hours) {
        this.personalResetHours = hours <= 0L ? 24L : hours;
    }

    public File fileOf(UUID id) {
        return new File(directory, id.toString() + ".yml");
    }

    @Override
    public PlayerProfile get(UUID id) {
        if (id == null) {
            return null;
        }
        PlayerProfile cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        PlayerProfile loaded = load(id);
        cache.put(id, loaded);
        index(loaded);
        return loaded;
    }

    @Override
    public PlayerProfile cached(UUID id) {
        return id == null ? null : cache.get(id);
    }

    @Override
    public PlayerProfile getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ENGLISH);
        UUID known = nameIndex.get(key);
        if (known != null) {
            PlayerProfile profile = cache.get(known);
            if (profile != null) {
                return profile;
            }
        }
        for (PlayerProfile profile : cache.values()) {
            if (key.equals(profile.name().toLowerCase(Locale.ENGLISH))) {
                return profile;
            }
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!isProfileFile(file)) {
                continue;
            }
            PlayerProfile profile = load(file);
            if (profile != null && key.equals(profile.name().toLowerCase(Locale.ENGLISH))) {
                return get(profile.uuid());
            }
        }
        return null;
    }

    @Override
    public void save(final PlayerProfile profile) {
        if (profile == null || profile.uuid() == null) {
            return;
        }
        if (!plugin.isEnabled()) {
            saveBlocking(profile);
            return;
        }
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    saveBlocking(profile);
                }
            });
        } catch (Throwable throwable) {
            saveBlocking(profile);
        }
    }

    @Override
    public void saveAllBlocking() {
        for (PlayerProfile profile : cache.values()) {
            saveBlocking(profile);
        }
    }

    @Override
    public Collection<PlayerProfile> all() {
        return Collections.unmodifiableCollection(new ArrayList<PlayerProfile>(cache.values()));
    }

    @Override
    public void unload(UUID id) {
        if (id == null) {
            return;
        }
        PlayerProfile profile = cache.remove(id);
        if (profile != null) {
            // only drop the name if it still points at THIS profile
            String name = key(profile.name());
            if (name.length() > 0 && id.equals(nameIndex.get(name))) {
                nameIndex.remove(name);
            }
        }
    }

    @Override
    public void saveBlocking(PlayerProfile profile) {
        if (profile == null || profile.uuid() == null) {
            return;
        }
        synchronized (profile) {
            YamlConfiguration yaml = new YamlConfiguration();
            write(profile, yaml);
            File file = fileOf(profile.uuid());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("[AurumBuyer] Could not create " + parent.getAbsolutePath());
            }
            try {
                yaml.save(file);
                profile.dirty(false);
            } catch (IOException exception) {
                plugin.getLogger().warning("[AurumBuyer] Failed to save " + file.getName() + ": " + exception.getMessage());
            }
        }
    }

    /** Reads every stored profile without caching it (used by top lists). */
    public List<LightProfile> loadAllLight() {
        List<LightProfile> out = new ArrayList<LightProfile>();
        File[] files = directory.listFiles();
        if (files == null) {
            return out;
        }
        for (File file : files) {
            if (!isProfileFile(file)) {
                continue;
            }
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                UUID id = parseUuid(yaml.getString("uuid"));
                if (id == null) {
                    id = parseUuid(stripExtension(file.getName()));
                }
                if (id == null) {
                    continue;
                }
                out.add(new LightProfile(id, yaml.getString("name", ""),
                        yaml.getDouble("money-total", 0.0D),
                        yaml.getDouble("points-total", 0.0D)));
            } catch (Throwable throwable) {
                plugin.getLogger().warning("[AurumBuyer] Failed to read " + file.getName() + ": " + throwable.getMessage());
            }
        }
        return out;
    }

    public File directory() {
        return directory;
    }

    // ------------------------------------------------------------------ loading

    private PlayerProfile load(UUID id) {
        File file = fileOf(id);
        if (file.isFile()) {
            PlayerProfile loaded = load(file);
            if (loaded != null) {
                loaded.resetPersonalLimitsIfDue(personalResetHours);
                return loaded;
            }
        }
        OfflinePlayer offline = null;
        try {
            offline = Bukkit.getOfflinePlayer(id);
        } catch (Throwable ignored) {
            // name lookup is optional
        }
        String name = offline == null ? "" : offline.getName();
        PlayerProfile created = new PlayerProfile(id, name == null ? "" : name, historySize);
        created.internalBalance(startBalance);
        created.personalResetAt(System.currentTimeMillis() + personalResetHours * 3600000L);
        created.dirty(true);
        return created;
    }

    private PlayerProfile load(File file) {
        YamlConfiguration yaml;
        try {
            yaml = YamlConfiguration.loadConfiguration(file);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[AurumBuyer] Failed to parse " + file.getName() + ": " + throwable.getMessage());
            return null;
        }
        UUID id = parseUuid(yaml.getString("uuid"));
        if (id == null) {
            id = parseUuid(stripExtension(file.getName()));
        }
        if (id == null) {
            return null;
        }
        PlayerProfile profile = new PlayerProfile(id, yaml.getString("name", ""), historySize);
        profile.moneyTotal(yaml.getDouble("money-total", 0.0D));
        profile.points(yaml.getDouble("points", 0.0D));
        profile.pointsTotal(yaml.getDouble("points-total", profile.points()));
        profile.ultaPoints(yaml.getDouble("ulta-points", 0.0D));
        profile.internalBalance(yaml.getDouble("internal-balance", startBalance));
        profile.lastUltaAt(yaml.getLong("last-ulta-at", 0L));
        profile.personalResetAt(yaml.getLong("personal-reset-at", 0L));
        profile.autoMode(AutoSellMode.parse(yaml.getString("auto-mode", "DISABLED")));
        profile.ignoreSlots(yaml.getBoolean("ignore-slots", true));
        profile.autoMessage(yaml.getBoolean("auto-message", true));

        ConfigurationSection sold = yaml.getConfigurationSection("sold");
        if (sold != null) {
            for (String key : sold.getKeys(false)) {
                profile.addSold(key, asLong(sold.get(key), 0L));
            }
        }
        ConfigurationSection purchases = yaml.getConfigurationSection("purchases");
        if (purchases != null) {
            for (String key : purchases.getKeys(false)) {
                profile.addPurchase(key, asInt(purchases.get(key), 0));
            }
        }
        ConfigurationSection levels = yaml.getConfigurationSection("booster-levels");
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                profile.setBoosterLevel(key, asInt(levels.get(key), 0));
            }
        }
        ConfigurationSection spent = yaml.getConfigurationSection("booster-spent");
        if (spent != null) {
            for (String key : spent.getKeys(false)) {
                profile.addBoosterSpent(key, asDouble(spent.get(key), 0.0D));
            }
        }
        ConfigurationSection limits = yaml.getConfigurationSection("personal-limits");
        if (limits != null) {
            for (String key : limits.getKeys(false)) {
                profile.setPersonalLimit(key, asLong(limits.get(key), 0L));
            }
        }
        ConfigurationSection boosts = yaml.getConfigurationSection("active-boosts");
        if (boosts != null) {
            for (String key : boosts.getKeys(false)) {
                ConfigurationSection boost = boosts.getConfigurationSection(key);
                if (boost != null) {
                    profile.setActiveBoost(key, boost.getDouble("percent", 0.0D), boost.getLong("until", 0L));
                } else {
                    profile.setActiveBoost(key, asDouble(boosts.get(key), 0.0D), 0L);
                }
            }
        }
        List<Map<?, ?>> storedHistory = yaml.getMapList("history");
        if (storedHistory != null) {
            for (int i = storedHistory.size() - 1; i >= 0; i--) {
                Map<?, ?> entry = storedHistory.get(i);
                if (entry == null) {
                    continue;
                }
                profile.addHistory(new HistoryEntry(
                        asLong(entry.get("time"), 0L),
                        String.valueOf(entry.get("item")),
                        asInt(entry.get("amount"), 0),
                        asDouble(entry.get("money"), 0.0D),
                        asDouble(entry.get("points"), 0.0D)));
            }
        }
        profile.dirty(false);
        return profile;
    }

    // ------------------------------------------------------------------ writing

    private void write(PlayerProfile profile, YamlConfiguration yaml) {
        yaml.set("uuid", profile.uuid() == null ? "" : profile.uuid().toString());
        yaml.set("name", profile.name());
        yaml.set("money-total", Double.valueOf(profile.moneyTotal()));
        yaml.set("points", Double.valueOf(profile.points()));
        yaml.set("points-total", Double.valueOf(profile.pointsTotal()));
        yaml.set("ulta-points", Double.valueOf(profile.ultaPoints()));
        yaml.set("internal-balance", Double.valueOf(profile.internalBalance()));
        yaml.set("last-ulta-at", Long.valueOf(profile.lastUltaAt()));
        yaml.set("personal-reset-at", Long.valueOf(profile.personalResetAt()));
        yaml.set("auto-mode", profile.autoMode() == null ? AutoSellMode.DISABLED.name() : profile.autoMode().name());
        yaml.set("ignore-slots", Boolean.valueOf(profile.ignoreSlots()));
        yaml.set("auto-message", Boolean.valueOf(profile.autoMessage()));

        ConfigurationSection sold = yaml.createSection("sold");
        for (Map.Entry<String, Long> entry : profile.soldMap().entrySet()) {
            if (entry.getValue() != null) {
                sold.set(entry.getKey(), entry.getValue());
            }
        }
        ConfigurationSection purchases = yaml.createSection("purchases");
        for (Map.Entry<String, Integer> entry : profile.purchaseMap().entrySet()) {
            if (entry.getValue() != null) {
                purchases.set(entry.getKey(), entry.getValue());
            }
        }
        ConfigurationSection levels = yaml.createSection("booster-levels");
        for (Map.Entry<String, Integer> entry : profile.boosterLevels().entrySet()) {
            if (entry.getValue() != null) {
                levels.set(entry.getKey(), entry.getValue());
            }
        }
        ConfigurationSection spent = yaml.createSection("booster-spent");
        for (Map.Entry<String, Double> entry : profile.boosterSpentMap().entrySet()) {
            if (entry.getValue() != null) {
                spent.set(entry.getKey(), entry.getValue());
            }
        }
        ConfigurationSection limits = yaml.createSection("personal-limits");
        for (Map.Entry<String, Long> entry : profile.personalLimitMap().entrySet()) {
            if (entry.getValue() != null) {
                limits.set(entry.getKey(), entry.getValue());
            }
        }
        ConfigurationSection boosts = yaml.createSection("active-boosts");
        for (Map.Entry<String, PlayerProfile.TempBoost> entry : profile.activeBoosts().entrySet()) {
            PlayerProfile.TempBoost boost = entry.getValue();
            if (boost == null) {
                continue;
            }
            ConfigurationSection section = boosts.createSection(entry.getKey());
            section.set("percent", Double.valueOf(boost.percent()));
            section.set("until", Long.valueOf(boost.until()));
        }

        List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
        for (HistoryEntry entry : profile.history()) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("time", Long.valueOf(entry.time()));
            map.put("item", entry.itemId());
            map.put("amount", Integer.valueOf(entry.amount()));
            map.put("money", Double.valueOf(entry.money()));
            map.put("points", Double.valueOf(entry.points()));
            history.add(map);
        }
        yaml.set("history", history);
    }

    // ------------------------------------------------------------------ helpers

    private void index(PlayerProfile profile) {
        if (profile != null && profile.name() != null && !profile.name().isEmpty()) {
            nameIndex.put(key(profile.name()), profile.uuid());
        }
    }

    private boolean isProfileFile(File file) {
        return file != null && file.isFile() && file.getName().toLowerCase(Locale.ENGLISH).endsWith(".yml");
    }

    private static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        return fileName.toLowerCase(Locale.ENGLISH).endsWith(".yml")
                ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static long asLong(Object raw, long def) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw == null) {
            return def;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    private static int asInt(Object raw, int def) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    private static double asDouble(Object raw, double def) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw == null) {
            return def;
        }
        try {
            return Double.parseDouble(String.valueOf(raw).trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            return def;
        }
    }
}