package com.xuvigan.aurumbuyer.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

/** Outcome of one sell call (SPEC.md section 4). */
public final class SellResult {

    private final double money;
    private final double points;
    private final double ulta;
    private final int itemCount;
    private final Map<Material, Integer> sold = new LinkedHashMap<Material, Integer>();
    private final List<String> limitHits = new ArrayList<String>();

    public SellResult(double money, double points, double ulta, int itemCount) {
        this.money = money;
        this.points = points;
        this.ulta = ulta;
        this.itemCount = itemCount;
    }

    public static SellResult none() {
        return new SellResult(0.0D, 0.0D, 0.0D, 0);
    }

    public void addSold(Material material, int amount) {
        if (material == null || amount <= 0) {
            return;
        }
        Integer current = sold.get(material);
        sold.put(material, Integer.valueOf((current == null ? 0 : current.intValue()) + amount));
    }

    public void addLimitHit(String entry) {
        if (entry != null && !entry.isEmpty()) {
            limitHits.add(entry);
        }
    }

    public void addLimitHit(String itemId, long left) {
        addLimitHit(itemId + "|" + left);
    }

    public double money() {
        return money;
    }

    public double points() {
        return points;
    }

    public double ulta() {
        return ulta;
    }

    public int itemCount() {
        return itemCount;
    }

    public Map<Material, Integer> sold() {
        return Collections.unmodifiableMap(sold);
    }

    public List<String> limitHits() {
        return Collections.unmodifiableList(limitHits);
    }

    public boolean empty() {
        return itemCount <= 0;
    }

    @Override
    public String toString() {
        return "SellResult{items=" + itemCount + ", money=" + money + ", points=" + points
                + ", ulta=" + ulta + ", limits=" + limitHits.size() + "}";
    }
}
