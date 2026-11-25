package com.xuvigan.aurumbuyer.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.data.PlayerProfile;

/**
 * "Множитель скупщика" (SPEC.md section 10.2): a percentage bonus that grows
 * with the money a player has sold during the whole server lifetime.
 */
public final class MultiplierService {

    private final ConfigManager config;
    private List<Double> tiers = new ArrayList<Double>();

    public MultiplierService(ConfigManager config) {
        this.config = config;
        reload();
    }

    public void reload() {
        List<Double> loaded = new ArrayList<Double>();
        if (config != null) {
            for (Double tier : config.multiplierTiers()) {
                if (tier != null) {
                    loaded.add(tier);
                }
            }
        }
        Collections.sort(loaded);
        this.tiers = Collections.unmodifiableList(loaded);
    }

    public boolean enabled() {
        return config != null && config.multiplierEnabled();
    }

    public List<Double> tiers() {
        return tiers;
    }

    /** 0-based index of the highest tier that is &lt;= moneyTotal, or -1. */
    public int tierIndex(double moneyTotal) {
        int index = -1;
        for (int i = 0; i < tiers.size(); i++) {
            Double tier = tiers.get(i);
            if (tier != null && moneyTotal + 0.0000001D >= tier.doubleValue()) {
                index = i;
            }
        }
        return index;
    }

    public double percent(PlayerProfile profile) {
        if (profile == null || config == null || !config.multiplierEnabled()) {
            return 0.0D;
        }
        double max = config.multiplierMaxPercent();
        double raw;
        if (!tiers.isEmpty()) {
            int index = tierIndex(profile.moneyTotal());
            raw = index < 0 ? 0.0D : config.multiplierPerStepPercent() * index;
        } else {
            double step = config.multiplierStep();
            if (step <= 0.0D) {
                return 0.0D;
            }
            raw = Math.floor(profile.moneyTotal() / step) * config.multiplierPerStepPercent();
        }
        raw = Math.max(0.0D, raw);
        if (max > 0.0D && raw > max) {
            return max;
        }
        return raw;
    }

    public double multiplier(PlayerProfile profile) {
        return 1.0D + percent(profile) / 100.0D;
    }
}
