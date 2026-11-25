package com.xuvigan.aurumbuyer.placeholder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.scheduler.BukkitTask;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.data.impl.YamlProfileStorage;

/**
 * Money / points leaderboards refreshed from disk (SPEC.md section 10.10).
 * The light reader does not touch the profile cache.
 */
public final class TopService {

    /** One leaderboard row. */
    public static final class Entry {

        private final String name;
        private final double amount;

        public Entry(String name, double amount) {
            this.name = name == null ? "" : name;
            this.amount = amount;
        }

        public String name() {
            return name;
        }

        public double amount() {
            return amount;
        }

        @Override
        public String toString() {
            return name + "=" + amount;
        }
    }

    private static final Comparator<YamlProfileStorage.LightProfile> MONEY =
            new Comparator<YamlProfileStorage.LightProfile>() {
                @Override
                public int compare(YamlProfileStorage.LightProfile left, YamlProfileStorage.LightProfile right) {
                    return Double.compare(right.moneyTotal(), left.moneyTotal());
                }
            };

    private static final Comparator<YamlProfileStorage.LightProfile> POINTS =
            new Comparator<YamlProfileStorage.LightProfile>() {
                @Override
                public int compare(YamlProfileStorage.LightProfile left, YamlProfileStorage.LightProfile right) {
                    return Double.compare(right.pointsTotal(), left.pointsTotal());
                }
            };

    private final AurumBuyer plugin;
    private volatile List<Entry> money = Collections.emptyList();
    private volatile List<Entry> points = Collections.emptyList();
    private BukkitTask task;
    private final java.util.concurrent.atomic.AtomicBoolean refreshing =
            new java.util.concurrent.atomic.AtomicBoolean();

    public TopService(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int refresh = plugin.config().topRefreshSeconds();
        long period = Math.max(10, refresh) * 20L;
        refreshAsync();
        try {
            this.task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    refreshNow();
                }
            }, period, period);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[AurumBuyer] Could not start the leaderboard task: " + throwable.getMessage());
        }
    }

    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // shutdown
            }
            this.task = null;
        }
    }

    public void refreshAsync() {
        try {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    refreshNow();
                }
            });
        } catch (Throwable throwable) {
            refreshNow();
        }
    }

    /** Recomputes both tables; safe from any thread (the YAML files are read only). */
    public void refreshNow() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!(plugin.profiles() instanceof YamlProfileStorage)) {
                return;
            }
            YamlProfileStorage storage = (YamlProfileStorage) plugin.profiles();
            List<YamlProfileStorage.LightProfile> loaded = new ArrayList<YamlProfileStorage.LightProfile>(storage.loadAllLight());
            int size = plugin.config().topSize();
            this.money = build(loaded, MONEY, size);
            this.points = build(loaded, POINTS, size);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[AurumBuyer] Leaderboard refresh failed: " + throwable.getMessage());
        } finally {
            refreshing.set(false);
        }
    }

    private static List<Entry> build(List<YamlProfileStorage.LightProfile> source,
                                     Comparator<YamlProfileStorage.LightProfile> comparator, int size) {
        List<YamlProfileStorage.LightProfile> copy = new ArrayList<YamlProfileStorage.LightProfile>(source);
        Collections.sort(copy, comparator);
        Map<String, Entry> unique = new LinkedHashMap<String, Entry>();
        for (YamlProfileStorage.LightProfile profile : copy) {
            if (profile == null) {
                continue;
            }
            String name = profile.name();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            if (unique.containsKey(name.toLowerCase(java.util.Locale.ENGLISH))) {
                continue;
            }
            double amount = comparator == MONEY ? profile.moneyTotal() : profile.pointsTotal();
            unique.put(name.toLowerCase(java.util.Locale.ENGLISH), new Entry(name, amount));
            if (unique.size() >= size) {
                break;
            }
        }
        return Collections.unmodifiableList(new ArrayList<Entry>(unique.values()));
    }

    public List<Entry> money() {
        return money;
    }

    public List<Entry> points() {
        return points;
    }

    /** 1-based lookup; returns null when the position does not exist. */
    public Entry money(int position) {
        return at(money, position);
    }

    public Entry points(int position) {
        return at(points, position);
    }

    private static Entry at(List<Entry> list, int position) {
        if (position <= 0 || list.size() < position) {
            return null;
        }
        return list.get(position - 1);
    }
}
