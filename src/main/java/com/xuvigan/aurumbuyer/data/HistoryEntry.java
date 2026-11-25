package com.xuvigan.aurumbuyer.data;

/** One sale record shown in the history menu. */
public final class HistoryEntry {

    private final long time;
    private final String itemId;
    private final int amount;
    private final double money;
    private final double points;

    public HistoryEntry(long time, String itemId, int amount, double money, double points) {
        this.time = time;
        this.itemId = itemId == null ? "" : itemId;
        this.amount = amount;
        this.money = money;
        this.points = points;
    }

    public long time() {
        return time;
    }

    public String itemId() {
        return itemId;
    }

    public int amount() {
        return amount;
    }

    public double money() {
        return money;
    }

    public double points() {
        return points;
    }

    public long getTime() {
        return time;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public double getMoney() {
        return money;
    }

    public double getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "HistoryEntry{" + itemId + " x" + amount + " for " + money + "}";
    }
}