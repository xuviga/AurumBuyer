package com.xuvigan.aurumbuyer.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mutable in-memory player profile; persisted by YamlProfileStorage. */
public final class PlayerProfile {

    /** Temporary booster boost (ulta / bonuses); survives restarts. */
    public static final class TempBoost {

        private final double percent;
        private final long until;

        public TempBoost(double percent, long until) {
            this.percent = percent;
            this.until = until;
        }

        public double percent() {
            return percent;
        }

        public long until() {
            return until;
        }

        public boolean expired() {
            return System.currentTimeMillis() >= until;
        }

        public long secondsLeft() {
            long left = (until - System.currentTimeMillis()) / 1000L;
            return left < 0L ? 0L : left;
        }
    }

    private final UUID uuid;
    private final int historySize;
    private final Map<String, Long> sold = new LinkedHashMap<String, Long>();
    private final Map<String, Integer> purchases = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> boosterLevels = new LinkedHashMap<String, Integer>();
    private final Map<String, Double> boosterSpent = new LinkedHashMap<String, Double>();
    private final Map<String, Long> personalLimits = new LinkedHashMap<String, Long>();
    private final Map<String, TempBoost> activeBoosts = new LinkedHashMap<String, TempBoost>();
    private final LinkedList<HistoryEntry> history = new LinkedList<HistoryEntry>();

    private String name;
    private double moneyTotal;
    private double points;
    private double pointsTotal;
    private double ultaPoints;
    private double internalBalance;
    private long personalResetAt;
    private long lastUltaAt;
    private AutoSellMode autoMode = AutoSellMode.DISABLED;
    private boolean ignoreSlots = true;
    private boolean autoMessage = true;
    private boolean dirty;

    public PlayerProfile(UUID uuid, String name) {
        this(uuid, name, 100);
    }

    public PlayerProfile(UUID uuid, String name, int historySize) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
        this.historySize = historySize <= 0 ? 100 : historySize;
    }

    public UUID uuid() {
        return uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String value) {
        if (value != null && !value.equals(name)) {
            this.name = value;
            this.dirty = true;
        }
    }

    public int historySize() {
        return historySize;
    }

    // ---------------------------------------------------------------- lifetime

    public double moneyTotal() {
        return moneyTotal;
    }

    public void moneyTotal(double value) {
        this.moneyTotal = value < 0.0D ? 0.0D : value;
    }

    public void addMoneyTotal(double amount) {
        if (amount == 0.0D) {
            return;
        }
        this.moneyTotal = Math.max(0.0D, this.moneyTotal + amount);
        this.dirty = true;
    }

    // ------------------------------------------------------------------ points

    public double points() {
        return points;
    }

    public void points(double value) {
        this.points = value < 0.0D ? 0.0D : value;
    }

    public void addPoints(double amount) {
        if (amount == 0.0D) {
            return;
        }
        this.points = Math.max(0.0D, this.points + amount);
        this.pointsTotal = Math.max(0.0D, this.pointsTotal + (amount > 0.0D ? amount : 0.0D));
        this.dirty = true;
    }

    public boolean takePoints(double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (this.points + 0.0000001D < amount) {
            return false;
        }
        this.points = Math.max(0.0D, this.points - amount);
        this.dirty = true;
        return true;
    }

    public double pointsTotal() {
        return pointsTotal;
    }

    public void pointsTotal(double value) {
        this.pointsTotal = value < 0.0D ? 0.0D : value;
    }

    // ------------------------------------------------------------------- ulta

    public double ultaPoints() {
        return ultaPoints;
    }

    public void ultaPoints(double value) {
        this.ultaPoints = value < 0.0D ? 0.0D : value;
    }

    public void addUltaPoints(double amount) {
        if (amount == 0.0D) {
            return;
        }
        this.ultaPoints = Math.max(0.0D, this.ultaPoints + amount);
        this.dirty = true;
    }

    public void addUltaPoints(double amount, double max) {
        addUltaPoints(amount);
        if (max > 0.0D && this.ultaPoints > max) {
            this.ultaPoints = max;
        }
    }

    public boolean takeUltaPoints(double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (this.ultaPoints + 0.0000001D < amount) {
            return false;
        }
        this.ultaPoints = Math.max(0.0D, this.ultaPoints - amount);
        this.dirty = true;
        return true;
    }

    public long lastUltaAt() {
        return lastUltaAt;
    }

    public void lastUltaAt(long value) {
        this.lastUltaAt = value;
        this.dirty = true;
    }

    // ----------------------------------------------------------------- boosters

    public int boosterLevel(String category) {
        Integer level = boosterLevels.get(key(category));
        return level == null ? 0 : level.intValue();
    }

    public void setBoosterLevel(String category, int level) {
        boosterLevels.put(key(category), Integer.valueOf(Math.max(0, level)));
        this.dirty = true;
    }

    public double boosterSpent(String category) {
        Double spent = boosterSpent.get(key(category));
        return spent == null ? 0.0D : spent.doubleValue();
    }

    public void addBoosterSpent(String category, double amount) {
        if (amount == 0.0D) {
            return;
        }
        String key = key(category);
        Double current = boosterSpent.get(key);
        boosterSpent.put(key, Double.valueOf((current == null ? 0.0D : current.doubleValue()) + amount));
        this.dirty = true;
    }

    public double boosterSpentTotal() {
        double total = 0.0D;
        for (Double value : boosterSpent.values()) {
            if (value != null) {
                total += value.doubleValue();
            }
        }
        return total;
    }

    public Map<String, Integer> boosterLevels() {
        return boosterLevels;
    }

    public Map<String, Double> boosterSpentMap() {
        return boosterSpent;
    }

    // ------------------------------------------------------------ temporary boost

    public Map<String, TempBoost> activeBoosts() {
        return Collections.unmodifiableMap(activeBoosts);
    }

    public TempBoost activeBoost(String category) {
        TempBoost boost = activeBoosts.get(key(category));
        if (boost != null && boost.expired()) {
            activeBoosts.remove(key(category));
            this.dirty = true;
            return null;
        }
        return boost;
    }

    public void setActiveBoost(String category, double percent, long until) {
        activeBoosts.put(key(category), new TempBoost(percent, until));
        this.dirty = true;
    }

    public void clearExpiredBoosts() {
        java.util.Iterator<Map.Entry<String, TempBoost>> iterator = activeBoosts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TempBoost> entry = iterator.next();
            if (entry.getValue() == null || entry.getValue().expired()) {
                iterator.remove();
                this.dirty = true;
            }
        }
    }

    // --------------------------------------------------------------- sold stats

    public long sold(String itemId) {
        Long value = sold.get(key(itemId));
        return value == null ? 0L : value.longValue();
    }

    public void addSold(String itemId, long amount) {
        if (amount == 0L) {
            return;
        }
        String key = key(itemId);
        Long current = sold.get(key);
        long next = (current == null ? 0L : current.longValue()) + amount;
        if (next <= 0L) {
            sold.remove(key);
        } else {
            sold.put(key, Long.valueOf(next));
        }
        this.dirty = true;
    }

    public Map<String, Long> soldMap() {
        return sold;
    }

    // ---------------------------------------------------------------- purchases

    public int purchases(String shopId) {
        Integer value = purchases.get(key(shopId));
        return value == null ? 0 : value.intValue();
    }

    public void addPurchase(String shopId, int amount) {
        if (amount == 0) {
            return;
        }
        String key = key(shopId);
        Integer current = purchases.get(key);
        int next = (current == null ? 0 : current.intValue()) + amount;
        if (next <= 0) {
            purchases.remove(key);
        } else {
            purchases.put(key, Integer.valueOf(next));
        }
        this.dirty = true;
    }

    public Map<String, Integer> purchaseMap() {
        return purchases;
    }

    // ----------------------------------------------------------- personal limits

    public long personalLimitConsumed(String itemId) {
        Long value = personalLimits.get(key(itemId));
        return value == null ? 0L : value.longValue();
    }

    public void consumePersonalLimit(String itemId, long amount) {
        if (amount == 0L) {
            return;
        }
        String key = key(itemId);
        Long current = personalLimits.get(key);
        long next = (current == null ? 0L : current.longValue()) + amount;
        if (next <= 0L) {
            personalLimits.remove(key);
        } else {
            personalLimits.put(key, Long.valueOf(next));
        }
        this.dirty = true;
    }

    public void setPersonalLimit(String itemId, long consumed) {
        String key = key(itemId);
        if (consumed <= 0L) {
            personalLimits.remove(key);
        } else {
            personalLimits.put(key, Long.valueOf(consumed));
        }
        this.dirty = true;
    }

    public Map<String, Long> personalLimitMap() {
        return personalLimits;
    }

    public long personalResetAt() {
        return personalResetAt;
    }

    public void personalResetAt(long value) {
        this.personalResetAt = value;
        this.dirty = true;
    }

    public void resetPersonalLimits() {
        personalLimits.clear();
        this.dirty = true;
    }

    /** Resets the personal limit counters when the configured period has passed. */
    public void resetPersonalLimitsIfDue(long resetHours) {
        long hours = resetHours <= 0L ? 24L : resetHours;
        long period = hours * 3600000L;
        long now = System.currentTimeMillis();
        if (personalResetAt <= 0L) {
            personalResetAt = now + period;
            this.dirty = true;
            return;
        }
        if (now >= personalResetAt) {
            personalLimits.clear();
            personalResetAt = now + period;
            this.dirty = true;
        }
    }

    // ------------------------------------------------------------------ history

    public List<HistoryEntry> history() {
        return Collections.unmodifiableList(history);
    }

    public void addHistory(HistoryEntry entry) {
        if (entry == null) {
            return;
        }
        history.addFirst(entry);
        while (history.size() > historySize) {
            history.removeLast();
        }
        this.dirty = true;
    }

    public void clearHistory() {
        history.clear();
        this.dirty = true;
    }

    // ----------------------------------------------------------------- settings

    public AutoSellMode autoMode() {
        return autoMode;
    }

    public void autoMode(AutoSellMode mode) {
        this.autoMode = mode == null ? AutoSellMode.DISABLED : mode;
        this.dirty = true;
    }

    public boolean ignoreSlots() {
        return ignoreSlots;
    }

    public void ignoreSlots(boolean value) {
        this.ignoreSlots = value;
        this.dirty = true;
    }

    public boolean autoMessage() {
        return autoMessage;
    }

    public void autoMessage(boolean value) {
        this.autoMessage = value;
        this.dirty = true;
    }

    // ---------------------------------------------------------- internal economy

    public double internalBalance() {
        return internalBalance;
    }

    public void internalBalance(double value) {
        this.internalBalance = value < 0.0D ? 0.0D : value;
        this.dirty = true;
    }

    public void addInternalBalance(double amount) {
        this.internalBalance = Math.max(0.0D, this.internalBalance + amount);
        this.dirty = true;
    }

    public boolean takeInternalBalance(double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (this.internalBalance + 0.0000001D < amount) {
            return false;
        }
        this.internalBalance = Math.max(0.0D, this.internalBalance - amount);
        this.dirty = true;
        return true;
    }

    // -------------------------------------------------------------------- misc

    public boolean dirty() {
        return dirty;
    }

    public void dirty(boolean value) {
        this.dirty = value;
    }

    /** Full admin reset: byte counters and statistics, identity is kept. */
    public void reset() {
        sold.clear();
        purchases.clear();
        boosterLevels.clear();
        boosterSpent.clear();
        personalLimits.clear();
        activeBoosts.clear();
        history.clear();
        moneyTotal = 0.0D;
        points = 0.0D;
        pointsTotal = 0.0D;
        ultaPoints = 0.0D;
        internalBalance = 0.0D;
        personalResetAt = 0L;
        lastUltaAt = 0L;
        autoMode = AutoSellMode.DISABLED;
        this.dirty = true;
    }

    private static String key(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(java.util.Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return "PlayerProfile{" + name + ", money=" + moneyTotal + ", points=" + points + "}";
    }
}