package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.booster.BoosterCategory;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;
import com.xuvigan.aurumbuyer.util.Numbers;
import com.xuvigan.aurumbuyer.util.TimeUtil;

/** Permanent boosters per category plus the ulta button. */
public final class BoosterMenu extends AbstractMenu {

    private static final int ULTA_SLOT = 49;
    private static final int BACK_SLOT = 45;

    public BoosterMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.BOOSTERS;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("boosters", plugin.messages().get("boosters-title"));
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        if (profile == null) {
            return;
        }
        int fallbackSlot = 11;
        for (BoosterCategory category : plugin.boosters().categories()) {
            int slot = category.slot() > 0 ? Math.min(53, category.slot()) : fallbackSlot;
            fallbackSlot += 1;
            set(holder, slot, item(category, profile, player));
            holder.action(slot, "BOOSTER:" + category.id());
        }
        set(holder, ULTA_SLOT, ulta(profile, player));
        holder.action(ULTA_SLOT, "ULTA");
        nav(holder, "back", BACK_SLOT, Material.ARROW, plugin.messages().get("back"));
    }

    private org.bukkit.inventory.ItemStack item(BoosterCategory category, PlayerProfile profile, Player player) {
        int level = plugin.boosters().level(profile, category.id());
        int maxLevel = category.maxLevel();
        boolean maxed = level >= maxLevel;
        double percent = plugin.boosters().percent(profile, category.id());
        double nextBonus = category.percentAt(Math.min(level + 1, maxLevel)) - category.percentAt(level);
        double nextCost = maxed ? 0.0D : plugin.boosters().nextCost(profile, category.id());
        long tempLeft = plugin.boosters().tempSecondsLeft(profile, category.id());

        List<String> lore = new ArrayList<String>();
        if (category.lore() != null && !category.lore().isEmpty()) {
            for (String line : category.lore()) {
                lore.add(line
                        .replace("%level%", String.valueOf(level))
                        .replace("%max_level%", String.valueOf(maxLevel))
                        .replace("%percent%", Numbers.round(percent))
                        .replace("%next_bonus%", Numbers.round(nextBonus))
                        .replace("%next_cost%", plugin.economy().format(nextCost))
                        .replace("%spent%", Numbers.round(profile.boosterSpent(category.id())))
                        .replace("{points}", plugin.messages().pointsName()));
            }
        } else {
            lore.add(plugin.messages().format("booster-level-line", "level", String.valueOf(level),
                    "max_level", String.valueOf(maxLevel)));
            lore.add(plugin.messages().format("booster-percent-line", "percent", Numbers.round(percent)));
            lore.add(plugin.messages().format("booster-next-line", "next_bonus", Numbers.round(nextBonus),
                    "next_cost", plugin.economy().format(nextCost)));
        }
        if (tempLeft > 0L) {
            lore.add(plugin.messages().format("booster-temp-line",
                    "temp", Numbers.round(plugin.boosters().tempPercent(profile, category.id())),
                    "time", TimeUtil.mmss(tempLeft)));
        }
        lore.add("");
        lore.add(maxed ? plugin.messages().get("booster-max") : plugin.messages().get("buy-hint"));
        lore = placeholders(player, lore);

        Material material = plugin.boosters().material(category.id());
        return ItemBuilder.of(material)
                .name(placeholders(player, category.display()))
                .lore(lore)
                .customModelData(category.customModelData())
                .glow(!maxed && profile.points() + 0.0000001D >= nextCost)
                .build();
    }

    private org.bukkit.inventory.ItemStack ulta(PlayerProfile profile, Player player) {
        double cost = plugin.config().ultaCost();
        long cooldown = plugin.boosters().ultaCooldownLeft(profile);
        List<String> lore = new ArrayList<String>();
        lore.add(plugin.messages().format("boosters-ulta-line",
                "ulta", Numbers.round(profile.ultaPoints()),
                "max", Numbers.round(plugin.config().ultaMaxPoints()),
                "cost", Numbers.round(cost)));
        if (cooldown > 0L) {
            lore.add(plugin.messages().format("booster-ulta-cooldown", "time", TimeUtil.mmss(cooldown)));
        } else if (profile.ultaPoints() + 0.0000001D < cost) {
            lore.add(plugin.messages().format("ultra-soon",
                    "ulta", Numbers.round(profile.ultaPoints()),
                    "max", Numbers.round(plugin.config().ultaMaxPoints()),
                    "need", Numbers.round(cost - profile.ultaPoints())));
        } else {
            lore.add(plugin.messages().format("ulta-ready-line",
                    "percent", Numbers.round(plugin.config().ultaPercent()),
                    "minutes", String.valueOf(plugin.config().ultaMinutes())));
        }
        return ItemBuilder.of(Material.BLAZE_POWDER)
                .name("&6&l" + plugin.messages().get("ultra-title"))
                .lore(placeholders(player, lore))
                .glow(plugin.boosters().canUlta(profile))
                .build();
    }
}
