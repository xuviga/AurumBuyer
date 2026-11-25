package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.booster.BoosterCategory;
import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;
import com.xuvigan.aurumbuyer.util.Numbers;
import com.xuvigan.aurumbuyer.util.TimeUtil;

/** Main menu: entry points plus a live summary card. */
public final class MainMenu extends AbstractMenu {

    public MainMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.MAIN;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("main", plugin.messages().get("main-title"));
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        ConfigManager config = plugin.config();
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());

        button(holder, "shop", "SHOP", 20, Material.CHEST, plugin.messages().get("main-shop"), player);
        button(holder, "history", "HISTORY", 21, Material.BOOK, plugin.messages().get("main-history"), player);
        button(holder, "settings", "SETTINGS", 22, Material.COMPARATOR, plugin.messages().get("main-settings"), player);
        button(holder, "boosters", "BOOSTERS", 23, Material.EXPERIENCE_BOTTLE, plugin.messages().get("main-boosters"), player);
        button(holder, "limits", "LIMITS", 24, Material.CLOCK, plugin.messages().get("main-limits"), player);

        int ultaSlot = config.menuItemSlot("main", "ulta", 40);
        Material ultaMaterial = config.menuItemMaterial("main", "ulta", Material.BLAZE_POWDER);
        String ultaName = config.menuItemName("main", "ulta", plugin.messages().get("ultra-title"));
        ItemBuilder ulta = ItemBuilder.of(ultaMaterial).name(ultaName)
                .customModelData(config.menuItemCustomModelData("main", "ulta"));
        List<String> lore = new ArrayList<String>();
        if (profile != null) {
            lore.add(plugin.messages().format("ulta-line",
                    "ulta", Numbers.round(profile.ultaPoints()),
                    "max", Numbers.round(config.ultaMaxPoints()),
                    "cost", Numbers.round(config.ultaCost())));
            long cooldown = plugin.boosters().ultaCooldownLeft(profile);
            if (cooldown > 0L) {
                lore.add(plugin.messages().format("booster-ulta-cooldown", "time", TimeUtil.mmss(cooldown)));
            } else {
                lore.add(plugin.messages().format("ulta-ready-line", "percent", Numbers.round(config.ultaPercent()),
                        "minutes", String.valueOf(config.ultaMinutes())));
            }
        }
        ulta.lore(placeholders(player, lore));
        ulta.glow(profile != null && plugin.boosters().canUlta(profile));
        set(holder, ultaSlot, ulta.build());
        holder.action(ultaSlot, "ULTA");

        int summarySlot = 49;
        set(holder, summarySlot, summary(profile, player));
        holder.action(summarySlot, "SUMMARY");

        int closeSlot = 53;
        set(holder, closeSlot, ItemBuilder.of(Material.BARRIER).name(plugin.messages().get("close")).build());
        holder.action(closeSlot, "CLOSE");
    }

    private ItemStack summary(PlayerProfile profile, Player player) {
        ItemBuilder builder = ItemBuilder.of(Material.PAPER).name(plugin.messages().get("summary-title"));
        if (profile == null) {
            return builder.build();
        }
        double percent = plugin.multipliers() == null ? 0.0D : plugin.multipliers().percent(profile);
        List<String> lore = new ArrayList<String>();
        lore.add(plugin.messages().format("multiplier-line", "percent", Numbers.round(percent)));
        lore.add(plugin.messages().format("points-line", "points", Numbers.round(profile.points())));
        lore.add(plugin.messages().format("total-money-line",
                "money", plugin.economy().format(profile.moneyTotal())));
        lore.add(plugin.messages().format("auto-mode-line", "mode", modeName(profile.autoMode())));
        builder.lore(placeholders(player, lore));
        return builder.build();
    }

    private String modeName(AutoSellMode mode) {
        if (mode == AutoSellMode.ENABLED) {
            return plugin.messages().get("auto-mode-value-enabled");
        }
        if (mode == AutoSellMode.UNTIL_QUIT) {
            return plugin.messages().get("auto-mode-value-until-quit");
        }
        return plugin.messages().get("auto-mode-value-disabled");
    }

    private void button(GuiHolder holder, String key, String action, int fallbackSlot, Material fallbackMaterial,
                        String fallbackName, Player player) {
        ConfigManager config = plugin.config();
        int slot = config.menuItemSlot("main", key, fallbackSlot);
        Material material = config.menuItemMaterial("main", key, fallbackMaterial);
        String name = config.menuItemName("main", key, fallbackName);
        ItemBuilder builder = ItemBuilder.of(material).name(name)
                .customModelData(config.menuItemCustomModelData("main", key));
        List<String> lore = config.colorList("gui.main.items." + key + ".lore");
        if (!lore.isEmpty()) {
            List<String> resolved = new ArrayList<String>();
            for (String line : lore) {
                resolved.add(plugin.placeholders().apply(player, line));
            }
            builder.lore(resolved);
        }
        builder.glow(config.menuItemGlow("main", key));
        int safeSlot = Math.max(0, Math.min(53, slot));
        set(holder, safeSlot, builder.build());
        holder.action(safeSlot, action);
    }
}
