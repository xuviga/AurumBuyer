package com.xuvigan.aurumbuyer.booster;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.config.BoosterConfig;
import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.config.PricesConfig;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.util.TimeUtil;

/**
 * Permanent boosters, the ulta temporary boost and their costs
 * (SPEC.md sections 10.5 and 10.6).
 */
public final class BoosterService {

    /** Outcome of {@link #buy(Player, String)}. */
    public enum BuyResult {
        OK,
        NO_MONEY,
        MAX_LEVEL
    }

    private final AurumBuyer plugin;

    public BoosterService(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    private BoosterConfig config() {
        return plugin.boosterConfig();
    }

    public List<BoosterCategory> categories() {
        List<BoosterCategory> out = new ArrayList<BoosterCategory>();
        BoosterConfig source = config();
        if (source == null) {
            return out;
        }
        for (BoosterConfig.Category category : source.categories()) {
            if (category == null) {
                continue;
            }
            out.add(new BoosterCategory(category.id(), category.display(), category.material().name(),
                    category.slot(), category.maxLevel(), category.baseCost(), category.growthPercent(),
                    category.percentPerLevel(), category.lore(), category.customModelData()));
        }
        return out;
    }

    public BoosterCategory category(String id) {
        BoosterConfig source = config();
        if (source == null || id == null) {
            return null;
        }
        BoosterConfig.Category raw = source.category(id);
        if (raw == null) {
            return null;
        }
        return new BoosterCategory(raw.id(), raw.display(), raw.material().name(), raw.slot(), raw.maxLevel(),
                raw.baseCost(), raw.growthPercent(), raw.percentPerLevel(), raw.lore(), raw.customModelData());
    }

    public boolean exists(String id) {
        return config() != null && config().category(id) != null;
    }

    public Material material(String id) {
        BoosterConfig source = config();
        if (source == null) {
            return Material.STONE;
        }
        BoosterConfig.Category raw = source.category(id);
        return raw == null || raw.material() == null ? Material.STONE : raw.material();
    }

    // ------------------------------------------------------------------ levels

    public int level(PlayerProfile profile, String cat) {
        return profile == null || cat == null ? 0 : profile.boosterLevel(cat);
    }

    public int maxLevel(String cat) {
        BoosterConfig source = config();
        return source == null ? 0 : source.maxLevel(cat);
    }

    /** Permanent percent of a category. */
    public double permanentPercent(PlayerProfile profile, String cat) {
        BoosterConfig source = config();
        if (source == null || cat == null) {
            return 0.0D;
        }
        return source.percentPerLevel(cat) * level(profile, cat);
    }

    /** Temporary (ulta) percent that applies to the category right now. */
    public double tempPercent(PlayerProfile profile, String cat) {
        if (profile == null || cat == null) {
            return 0.0D;
        }
        PlayerProfile.TempBoost boost = profile.activeBoost(cat);
        return boost == null ? 0.0D : boost.percent();
    }

    public long tempSecondsLeft(PlayerProfile profile, String cat) {
        if (profile == null || cat == null) {
            return 0L;
        }
        PlayerProfile.TempBoost boost = profile.activeBoost(cat);
        return boost == null ? 0L : boost.secondsLeft();
    }

    /** Permanent + temporary percent. */
    public double percent(PlayerProfile profile, String cat) {
        return permanentPercent(profile, cat) + tempPercent(profile, cat);
    }

    public double multiplier(PlayerProfile profile, String cat) {
        return 1.0D + percent(profile, cat) / 100.0D;
    }

    public double cost(String cat, int level) {
        BoosterConfig source = config();
        return source == null ? 0.0D : source.cost(cat, level);
    }

    public double nextCost(PlayerProfile profile, String cat) {
        if (isMax(profile, cat)) {
            return 0.0D;
        }
        return cost(cat, level(profile, cat));
    }

    public boolean isMax(PlayerProfile profile, String cat) {
        int max = maxLevel(cat);
        return max <= 0 || level(profile, cat) >= max;
    }

    public String display(String cat) {
        BoosterCategory category = category(cat);
        return category == null ? String.valueOf(cat) : category.display();
    }

    // -------------------------------------------------------------------- buy

    public BuyResult buy(Player player, String cat) {
        if (player == null || cat == null) {
            return BuyResult.MAX_LEVEL;
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        if (profile == null) {
            return BuyResult.MAX_LEVEL;
        }
        BoosterConfig source = config();
        if (source == null || source.category(cat) == null) {
            return BuyResult.MAX_LEVEL;
        }
        String key = cat.toLowerCase(Locale.ENGLISH);
        synchronized (profile) {
            if (isMax(profile, key)) {
                return BuyResult.MAX_LEVEL;
            }
            int current = profile.boosterLevel(key);
            double price = cost(key, current);
            if (price <= 0.0D) {
                return BuyResult.MAX_LEVEL;
            }
            if (!profile.takePoints(price)) {
                return BuyResult.NO_MONEY;
            }
            profile.setBoosterLevel(key, current + 1);
            profile.addBoosterSpent(key, price);
        }
        plugin.profiles().save(profile);
        return BuyResult.OK;
    }

    // ------------------------------------------------------------------- ulta

    /** Seconds left before the ulta may be activated again. */
    public long ultaCooldownLeft(PlayerProfile profile) {
        ConfigManager config = plugin.config();
        if (profile == null || config == null) {
            return 0L;
        }
        long cooldown = TimeUtil.millisFromMinutes(config.ultaCooldownMinutes());
        if (cooldown <= 0L || profile.lastUltaAt() <= 0L) {
            return 0L;
        }
        long left = (profile.lastUltaAt() + cooldown - System.currentTimeMillis()) / 1000L;
        return left < 0L ? 0L : left;
    }

    public boolean canUlta(Player player) {
        if (player == null) {
            return false;
        }
        ConfigManager config = plugin.config();
        if (!config.ultaEnabled()) {
            return false;
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        return canUlta(profile);
    }

    public boolean canUlta(PlayerProfile profile) {
        ConfigManager config = plugin.config();
        if (profile == null || !config.ultaEnabled()) {
            return false;
        }
        if (ultaCooldownLeft(profile) > 0L) {
            return false;
        }
        return profile.ultaPoints() + 0.0000001D >= config.ultaCost();
    }

    public boolean buyUltaBoost(Player player) {
        if (player == null) {
            return false;
        }
        ConfigManager config = plugin.config();
        if (!config.ultaEnabled()) {
            return false;
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        if (profile == null || !config.ultaEnabled()) {
            return false;
        }
        if (ultaCooldownLeft(profile) > 0L) {
            return false;
        }
        long until = System.currentTimeMillis() + TimeUtil.millisFromMinutes(config.ultaMinutes());
        double percent = config.ultaPercent();
        synchronized (profile) {
            if (!profile.takeUltaPoints(config.ultaCost())) {
                return false;
            }
            // the ulta bonus applies to every category, including categories that
            // come from prices.yml but have no permanent booster line
            Set<String> ids = new LinkedHashSet<String>();
            if (plugin.boosterConfig() != null) {
                ids.addAll(plugin.boosterConfig().ids());
            }
            PricesConfig prices = plugin.prices();
            if (prices != null) {
                for (PricesConfig.ItemWorth worth : prices.all()) {
                    if (worth != null && worth.category() != null && !worth.category().isEmpty()) {
                        ids.add(worth.category());
                    }
                }
            }
            if (ids.isEmpty()) {
                ids.add("ores");
                ids.add("farming");
                ids.add("monster");
            }
            for (String id : ids) {
                if (id == null) {
                    continue;
                }
                PlayerProfile.TempBoost existing = profile.activeBoost(id);
                double base = existing == null ? 0.0D : existing.percent();
                profile.setActiveBoost(id, base + percent, until);
            }
            profile.lastUltaAt(System.currentTimeMillis());
        }
        plugin.profiles().save(profile);
        return true;
    }

    /** Clears expired temporary boosts (housekeeping before saving). */
    public void cleanup(PlayerProfile profile) {
        if (profile != null) {
            profile.clearExpiredBoosts();
        }
    }
}
