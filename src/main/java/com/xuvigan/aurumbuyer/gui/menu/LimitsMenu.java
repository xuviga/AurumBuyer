package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.config.PricesConfig;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;
import com.xuvigan.aurumbuyer.util.TimeUtil;

/** Global + personal limit overview. */
public final class LimitsMenu extends AbstractMenu {

    private static final int BACK_SLOT = 49;

    public LimitsMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.LIMITS;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("limits", plugin.messages().get("limits-title"));
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        PricesConfig prices = plugin.prices();
        List<PricesConfig.ItemWorth> limited = new ArrayList<PricesConfig.ItemWorth>();
        if (prices != null) {
            for (PricesConfig.ItemWorth worth : prices.all()) {
                if (worth != null && worth.limited()) {
                    limited.add(worth);
                }
            }
        }
        if (limited.isEmpty()) {
            set(holder, 22, ItemBuilder.of(Material.COBWEB).name(plugin.messages().get("limits-empty")).build());
        } else {
            int slot = 0;
            for (int index = 0; index < limited.size() && slot < 45; index++, slot++) {
                PricesConfig.ItemWorth worth = limited.get(index);
                set(holder, slot, card(worth, profile, player));
            }
        }
        nav(holder, "back", BACK_SLOT, Material.ARROW, plugin.messages().get("back"));
    }

    private org.bukkit.inventory.ItemStack card(PricesConfig.ItemWorth worth, PlayerProfile profile, Player player) {
        Material material = Material.matchMaterial(worth.id());
        if (material == null || material == Material.AIR) {
            material = Material.PAPER;
        }
        List<String> lore = new ArrayList<String>();
        if (worth.globalEnabled()) {
            long used = plugin.limits().consumed(worth.id());
            lore.add(plugin.messages().format("limits-global-line",
                    "used", String.valueOf(used),
                    "limit", String.valueOf(worth.globalLimit())));
            lore.add(plugin.messages().format("limits-reset-line",
                    "time", TimeUtil.mmss(plugin.limits().secondsUntilGlobalReset(worth.id()))));
        }
        if (worth.personalEnabled() && profile != null) {
            long used = plugin.limits().personalConsumed(profile, worth.id());
            lore.add(plugin.messages().format("limits-personal-line",
                    "used", String.valueOf(used),
                    "limit", String.valueOf(worth.personalLimit())));
        }
        lore.add("");
        lore.add(plugin.messages().get("limit-price-line") + plugin.economy().format(worth.price()));
        return ItemBuilder.of(material)
                .name(placeholders(player, "&f" + prettyName(worth.id())))
                .lore(placeholders(player, lore))
                .customModelData(worth.customModelData())
                .build();
    }

    private static String prettyName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "?";
        }
        String lower = raw.toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
