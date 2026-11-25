package com.xuvigan.aurumbuyer.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.config.PricesConfig;
import com.xuvigan.aurumbuyer.data.HistoryEntry;
import com.xuvigan.aurumbuyer.data.PlayerProfile;

/**
 * Turns item stacks into money, Aurum points and ulta points
 * (SPEC.md section 10.1).
 *
 * <p>Price formula:
 * {@code unit = price * buyerMultiplier * boosterMultiplier * limitMultiplier}.
 * The booster multiplier combines the permanent category level and the
 * temporary ulta boost stored in the profile.</p>
 *
 * <p>All profile mutations happen while holding the profile monitor so that
 * the auto-sell task and a manual sale can never double-count a stack.</p>
 */
public final class SellEngine {

    private final AurumBuyer plugin;

    public SellEngine(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    /**
     * Sells the given stacks; {@code slots} holds the inventory slot of every
     * stack so sold stacks can be cleared in place.
     */
    private SellResult sell(Player player, List<ItemStack> items, List<Integer> slots, SellSource source) {
        if (player == null || items == null || items.isEmpty()) {
            return SellResult.none();
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        if (profile == null) {
            return SellResult.none();
        }
        PricesConfig prices = plugin.prices();
        if (prices == null || prices.isEmpty()) {
            return SellResult.none();
        }
        profile.name(player.getName());

        ConfigManager config = plugin.config();
        double tierMultiplier = plugin.multipliers() == null ? 1.0D : plugin.multipliers().multiplier(profile);
        double pointsPercent = config.sellPointsPercent();
        double ultaPercent = config.ultaEnabled() ? config.ultaPointsPercent() : 0.0D;
        double ultaMax = config.ultaMaxPoints();
        int maxPerCycle = source == SellSource.AUTO ? config.autoSellMaxItemsPerCycle() : Integer.MAX_VALUE;
        Inventory inventory = player.getInventory();
        // booster percent is constant for the whole sale, so resolve each
        // category only once instead of once per stack
        Map<String, Double> boosterCache = new LinkedHashMap<String, Double>();

        double money = 0.0D;
        int itemCount = 0;
        int processed = 0;
        Map<Material, Integer> soldByMaterial = new LinkedHashMap<Material, Integer>();
        Map<Material, Double> moneyByMaterial = new LinkedHashMap<Material, Double>();
        List<String> limitHits = new ArrayList<String>();

        synchronized (profile) {
            for (int index = 0; index < items.size(); index++) {
                ItemStack stack = items.get(index);
                if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                    continue;
                }
                PricesConfig.ItemWorth worth = prices.resolve(stack);
                if (worth == null || worth.price() <= 0.0D) {
                    continue;
                }
                String itemId = stack.getType().name();
                // limits are tracked per price entry: wildcard groups share one
                // counter (e.g. all "*_LOG" materials), explicit items use their id
                String limitKey = worth.id() == null || worth.id().isEmpty() ? itemId : worth.id();
                int amount = stack.getAmount();
                long allowed = amount;
                double limitedMultiplier = 1.0D;

                if (worth.limited()) {
                    if (worth.globalEnabled()) {
                        limitedMultiplier = Math.max(limitedMultiplier, worth.globalMultiplier());
                        long left = plugin.limits().remaining(limitKey, worth.globalLimit());
                        if (left < allowed) {
                            allowed = left;
                            limitHits.add(hit(itemId, left));
                        }
                    }
                    if (worth.personalEnabled() && allowed > 0L) {
                        limitedMultiplier = Math.max(limitedMultiplier, worth.personalMultiplier());
                        long left = plugin.limits().personalRemaining(profile, limitKey, worth.personalLimit());
                        if (left < allowed) {
                            allowed = left;
                            limitHits.add(hit(itemId, left));
                        }
                    }
                }

                if (allowed <= 0L) {
                    continue;
                }
                if (maxPerCycle != Integer.MAX_VALUE) {
                    long cap = (long) maxPerCycle - (long) processed;
                    if (cap <= 0L) {
                        break;
                    }
                    if (cap < allowed) {
                        allowed = cap;
                    }
                }

                int soldAmount = (int) Math.min(allowed, (long) amount);
                String category = worth.category();
                Double cachedBooster = boosterCache.get(category);
                if (cachedBooster == null) {
                    cachedBooster = Double.valueOf(plugin.boosters() == null
                            ? 1.0D : plugin.boosters().multiplier(profile, category));
                    boosterCache.put(category, cachedBooster);
                }
                double unit = worth.price() * tierMultiplier * cachedBooster.doubleValue() * limitedMultiplier;
                money += unit * soldAmount;
                itemCount += soldAmount;
                processed += soldAmount;

                if (worth.globalEnabled()) {
                    plugin.limits().consume(limitKey, soldAmount);
                }
                if (worth.personalEnabled()) {
                    plugin.limits().consumePersonal(profile, limitKey, soldAmount);
                }
                profile.addSold(itemId, soldAmount);
                Integer known = soldByMaterial.get(stack.getType());
                soldByMaterial.put(stack.getType(), Integer.valueOf((known == null ? 0 : known.intValue()) + soldAmount));
                Double knownMoney = moneyByMaterial.get(stack.getType());
                moneyByMaterial.put(stack.getType(), Double.valueOf(
                        (knownMoney == null ? 0.0D : knownMoney.doubleValue()) + unit * soldAmount));

                // clear the real inventory slot (or shrink the stack in place
                // when the caller did not provide slot indices)
                int slot = slots != null && index < slots.size() ? slots.get(index).intValue() : -1;
                if (soldAmount >= amount) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, null);
                    } else {
                        stack.setAmount(0);
                    }
                } else {
                    stack.setAmount(amount - soldAmount);
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, stack);
                    }
                }
            }
        }

        if (itemCount <= 0 || money <= 0.0D) {
            SellResult limited = new SellResult(0.0D, 0.0D, 0.0D, 0);
            copyDetails(limited, soldByMaterial, limitHits);
            return limited;
        }

        double points = Math.max(0.0D, money * pointsPercent / 100.0D);
        double ulta = ultaPercent > 0.0D ? Math.max(0.0D, money * ultaPercent / 100.0D) : 0.0D;
        SellResult result = new SellResult(money, points, ulta, itemCount);
        copyDetails(result, soldByMaterial, limitHits);

        long now = System.currentTimeMillis();
        synchronized (profile) {
            profile.addMoneyTotal(money);
            if (points > 0.0D) {
                profile.addPoints(points);
            }
            if (ulta > 0.0D) {
                profile.addUltaPoints(ulta, ultaMax);
            }
            for (Map.Entry<Material, Integer> entry : soldByMaterial.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().intValue() <= 0) {
                    continue;
                }
                Double itemMoney = moneyByMaterial.get(entry.getKey());
                double entryMoney = itemMoney == null ? 0.0D : itemMoney.doubleValue();
                double entryPoints = money > 0.0D ? points * (entryMoney / money) : 0.0D;
                profile.addHistory(new HistoryEntry(now, entry.getKey().name(), entry.getValue().intValue(),
                        entryMoney, entryPoints));
            }
        }

        if (plugin.economy() != null) {
            plugin.economy().deposit(player, money);
        }
        // persist consumed limits soon after a sale, without writing per stack
        plugin.limits().flush();
        plugin.profiles().save(profile);
        return result;
    }

    private static void copyDetails(SellResult target, Map<Material, Integer> soldByMaterial, List<String> limitHits) {
        for (Map.Entry<Material, Integer> entry : soldByMaterial.entrySet()) {
            if (entry.getValue() != null) {
                target.addSold(entry.getKey(), entry.getValue().intValue());
            }
        }
        for (String limitHit : limitHits) {
            target.addLimitHit(limitHit);
        }
    }

    /** Sells what {@link #collect(Player, PlayerProfile)} reports; honours ignore-slots. */
    public SellResult sellInventory(Player player, SellSource source) {
        if (player == null) {
            return SellResult.none();
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        List<Integer> slots = new ArrayList<Integer>();
        List<ItemStack> stacks = collect(player, profile, slots);
        if (stacks.isEmpty()) {
            return SellResult.none();
        }
        return sell(player, stacks, slots, source);
    }

    /**
     * Number of single items a whole-inventory sale would sell right now.
     * Used by auto-sell to honour {@code auto-sell.min-items} before selling.
     */
    public int sellableCount(Player player, PlayerProfile profile) {
        PricesConfig prices = plugin.prices();
        if (player == null || prices == null || prices.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : collect(player, profile)) {
            if (stack == null) {
                continue;
            }
            PricesConfig.ItemWorth worth = prices.resolve(stack);
            if (worth != null && worth.price() > 0.0D) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Stacks sold by a whole-inventory sale: the player storage contents only,
     * minus {@code auto-sell.ignore-slots} when the profile asks for it.
     */
    public List<ItemStack> collect(Player player, PlayerProfile profile) {
        return collect(player, profile, null);
    }

    private List<ItemStack> collect(Player player, PlayerProfile profile, List<Integer> slotOut) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (player == null) {
            return out;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        if (contents == null) {
            return out;
        }
        List<Integer> ignored = plugin.config().autoSellIgnoreSlots();
        boolean respectSlots = profile != null && profile.ignoreSlots();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (respectSlots && ignored.contains(Integer.valueOf(slot))) {
                continue;
            }
            out.add(stack);
            if (slotOut != null) {
                slotOut.add(Integer.valueOf(slot));
            }
        }
        return out;
    }

    private static String hit(String itemId, long left) {
        return itemId + "|" + Math.max(0L, left);
    }
}
