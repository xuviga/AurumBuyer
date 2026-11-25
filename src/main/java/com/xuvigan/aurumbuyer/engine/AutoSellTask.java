package com.xuvigan.aurumbuyer.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.util.Numbers;
import com.xuvigan.aurumbuyer.util.SoundUtil;

/** Periodic auto-sell (SPEC.md section 10.3). */
public final class AutoSellTask extends BukkitRunnable {

    private static final long LIMIT_NOTICE_INTERVAL_MS = 60000L;
    private static final long LIMIT_NOTICE_TTL_MS = 600000L;

    private final AurumBuyer plugin;
    private final Map<UUID, Long> limitNotices = new HashMap<UUID, Long>();

    public AutoSellTask(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.config().autoSellEnabled()) {
            return;
        }
        boolean requirePermission = plugin.config().autoSellRequirePermission();
        int minItems = plugin.config().autoSellMinItems();
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Long>> iterator = limitNotices.entrySet().iterator(); iterator.hasNext();) {
            if (now - iterator.next().getValue().longValue() > LIMIT_NOTICE_TTL_MS) {
                iterator.remove();
            }
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            PlayerProfile profile = plugin.profiles().cached(player.getUniqueId());
            // both ENABLED and UNTIL_QUIT are active modes
            if (profile == null || !profile.autoMode().active()) {
                continue;
            }
            if (requirePermission && !player.hasPermission(plugin.config().autoSellPermission())) {
                continue;
            }
            try {
                // min-items is a gate: do not sell at all when the inventory
                // holds fewer sellable items than configured
                if (minItems > 1 && plugin.sell().sellableCount(player, profile) < minItems) {
                    continue;
                }
                SellResult result = plugin.sell().sellInventory(player, SellSource.AUTO);
                if (result.empty()) {
                    if (profile.autoMessage() && !result.limitHits().isEmpty()) {
                        Long last = limitNotices.get(player.getUniqueId());
                        if (last == null || now - last.longValue() >= LIMIT_NOTICE_INTERVAL_MS) {
                            limitNotices.put(player.getUniqueId(), Long.valueOf(now));
                            sendLimits(player, result);
                        }
                    }
                    continue;
                }
                if (profile.autoMessage()) {
                    plugin.messages().send(player, "auto-sell-notify",
                            "count", String.valueOf(result.itemCount()),
                            "money", plugin.economy().format(result.money()),
                            "points", Numbers.round(result.points()));
                    sendLimits(player, result);
                    playSound(player, "sell");
                }
            } catch (Throwable throwable) {
                plugin.getLogger().warning("[AurumBuyer] Auto-sell failed for " + player.getName() + ": "
                        + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            }
        }
    }

    private void sendLimits(Player player, SellResult result) {
        List<String> hits = result.limitHits();
        for (String hit : hits) {
            int separator = hit.lastIndexOf('|');
            String item = separator > 0 ? hit.substring(0, separator) : hit;
            String left = separator > 0 ? hit.substring(separator + 1) : "0";
            plugin.messages().send(player, "sell-limit-hit", "item", item, "left", left);
        }
    }

    private void playSound(Player player, String key) {
        if (plugin.config().soundEnabled()) {
            SoundUtil.play(player, plugin.config().sound(key));
        }
    }
}
