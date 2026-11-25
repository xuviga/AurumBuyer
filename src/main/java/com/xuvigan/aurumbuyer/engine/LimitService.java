package com.xuvigan.aurumbuyer.engine;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.data.PlayerProfile;

/**
 * Global + personal item limits ("лимитки", SPEC.md section 10.4).
 *
 * <p>Global consumption lives in {@code globallimits.yml}
 * ({@code items.<ID>.consumed}, {@code items.<ID>.reset-at}) and is flushed
 * to disk asynchronously. Personal consumption lives inside the player
 * profile and resets itself when {@code limits.personal-reset-hours} passed.</p>
 */
public final class LimitService {

    private final Plugin plugin;
    private final ConfigManager config;
    private final File file;
    private final Map<String, Long> consumed = new LinkedHashMap<String, Long>();
    private final Map<String, Long> resetAt = new LinkedHashMap<String, Long>();

    private boolean dirty;
    private long globalResetHours = 24L;
    private long lastFlushAt;

    public LimitService(Plugin plugin, ConfigManager config, File file) {
        this.plugin = plugin;
        this.config = config;
        this.file = file == null ? new File(plugin.getDataFolder(), "globallimits.yml") : file;
        load();
    }

    public void reload() {
        this.consumed.clear();
        this.resetAt.clear();
        this.dirty = false;
        load();
    }

    // ------------------------------------------------------------------ storage

    private void load() {
        this.globalResetHours = config == null ? 24L : config.globalResetHours();
        if (file == null || !file.isFile()) {
            return;
        }
        YamlConfiguration yaml;
        try {
            yaml = YamlConfiguration.loadConfiguration(file);
        } catch (Throwable throwable) {
            warn("Failed to read " + file.getName() + ": " + throwable.getMessage());
            return;
        }
        ConfigurationSection section = yaml.getConfigurationSection("items");
        if (section == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long period = globalResetHours * 3600000L;
        for (String key : section.getKeys(false)) {
            String id = normalize(key);
            ConfigurationSection entry = section.getConfigurationSection(key);
            long stored;
            long reset;
            if (entry != null) {
                stored = entry.getLong("consumed", 0L);
                reset = entry.getLong("reset-at", 0L);
            } else {
                stored = yaml.getLong("items." + key, 0L);
                reset = 0L;
            }
            if (reset > 0L && now >= reset) {
                stored = 0L;
                reset = now + period;
                this.dirty = true;
            }
            if (stored > 0L) {
                consumed.put(id, Long.valueOf(stored));
            }
            resetAt.put(id, Long.valueOf(reset <= 0L ? now + period : reset));
        }
    }

    private void save() {
        if (!dirty) {
            return;
        }
        final YamlConfiguration snapshot = snapshot();
        // Consumed counters and reset dates are only touched on the main thread;
        // a background writer could let an older snapshot land after a newer one
        // and resurrect expired counters. The file is tiny, so it is written
        // inline instead (throttled by the `dirty` flag).
        if (write(snapshot)) {
            dirty = false;
        }
    }

    private YamlConfiguration snapshot() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("reset-hours", Long.valueOf(globalResetHours));
        yaml.set("updated-at", Long.valueOf(System.currentTimeMillis()));
        for (Map.Entry<String, Long> entry : consumed.entrySet()) {
            String base = "items." + entry.getKey();
            yaml.set(base + ".consumed", entry.getValue());
            Long reset = resetAt.get(entry.getKey());
            yaml.set(base + ".reset-at", reset == null ? Long.valueOf(0L) : reset);
        }
        return yaml;
    }

    private boolean write(YamlConfiguration yaml) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yaml.save(file);
            return true;
        } catch (IOException exception) {
            warn("Failed to save " + file.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    /** Flushes pending global limit changes (plugin disable / admin save). */
    public void saveBlocking() {
        save();
    }

    /**
     * Cheap periodic flush used after sales and by the autosave task.
     * Throttled so a burst of sales cannot write the file once per stack.
     */
    public void flush() {
        if (!dirty) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFlushAt < 5000L) {
            return;
        }
        lastFlushAt = now;
        save();
    }

    // ------------------------------------------------------------ global limits

    public long globalResetHours() {
        return globalResetHours;
    }

    /** Refreshes the whole table; ids missing from prices.yml are dropped when expired. */
    public void resetGlobalIfDue() {
        long now = System.currentTimeMillis();
        long period = globalResetHours * 3600000L;
        for (Map.Entry<String, Long> entry : new LinkedHashMap<String, Long>(consumed).entrySet()) {
            Long reset = resetAt.get(entry.getKey());
            if (reset == null || now >= reset.longValue()) {
                consumed.remove(entry.getKey());
                resetAt.put(entry.getKey(), Long.valueOf(now + period));
                dirty = true;
            }
        }
    }

    public long consumed(String itemId) {
        String key = normalize(itemId);
        Long reset = resetAt.get(key);
        if (reset != null && System.currentTimeMillis() >= reset.longValue()) {
            consumed.remove(key);
            resetAt.put(key, Long.valueOf(System.currentTimeMillis() + globalResetHours * 3600000L));
            dirty = true;
        }
        Long value = consumed.get(key);
        return value == null ? 0L : value.longValue();
    }

    public long remaining(String itemId, long limit) {
        if (limit <= 0L) {
            return Long.MAX_VALUE;
        }
        long left = limit - consumed(itemId);
        return left < 0L ? 0L : left;
    }

    public void consume(String itemId, long amount) {
        if (itemId == null || amount <= 0L) {
            return;
        }
        String id = normalize(itemId);
        long next = consumed(id) + amount;
        consumed.put(id, Long.valueOf(next));
        if (!resetAt.containsKey(id)) {
            resetAt.put(id, Long.valueOf(System.currentTimeMillis() + globalResetHours * 3600000L));
        }
        // written by the periodic flush() / saveBlocking(), never per sold stack
        dirty = true;
    }

    public void resetGlobal(String itemId) {
        if (itemId == null) {
            return;
        }
        String id = normalize(itemId);
        consumed.remove(id);
        dirty = true;
        save();
    }

    public void resetGlobalAll() {
        consumed.clear();
        resetAt.clear();
        dirty = true;
        save();
    }

    public long secondsUntilGlobalReset(String itemId) {
        resetGlobalIfDue();
        Long reset = resetAt.get(normalize(itemId));
        if (reset == null) {
            return globalResetHours * 3600L;
        }
        long left = (reset.longValue() - System.currentTimeMillis()) / 1000L;
        return left < 0L ? 0L : left;
    }

    // ---------------------------------------------------------- personal limits

    public long personalConsumed(PlayerProfile profile, String itemId) {
        if (profile == null) {
            return 0L;
        }
        profile.resetPersonalLimitsIfDue(personalResetHours());
        return profile.personalLimitConsumed(itemId);
    }

    public long personalRemaining(PlayerProfile profile, String itemId, long limit) {
        if (limit <= 0L) {
            return Long.MAX_VALUE;
        }
        long left = limit - personalConsumed(profile, itemId);
        return left < 0L ? 0L : left;
    }

    public void consumePersonal(PlayerProfile profile, String itemId, long amount) {
        if (profile == null || amount <= 0L) {
            return;
        }
        profile.resetPersonalLimitsIfDue(personalResetHours());
        profile.consumePersonalLimit(itemId, amount);
        if (profile.personalResetAt() <= 0L) {
            profile.personalResetAt(System.currentTimeMillis() + personalResetHours() * 3600000L);
        }
    }

    public void resetPersonal(PlayerProfile profile) {
        if (profile == null) {
            return;
        }
        profile.resetPersonalLimits();
        profile.personalResetAt(System.currentTimeMillis() + personalResetHours() * 3600000L);
    }

    public long personalResetHours() {
        return config == null ? 24L : config.personalResetHours();
    }

    private void warn(String message) {
        if (plugin != null) {
            plugin.getLogger().warning("[AurumBuyer] " + message);
        }
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(java.util.Locale.ENGLISH);
    }
}
