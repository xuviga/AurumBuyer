package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;
import com.xuvigan.aurumbuyer.placeholder.TopService;

/** Operator dashboard (read only): totals and the money / points leaderboards. */
public final class AdminMenu extends AbstractMenu {

    private static final int BACK_SLOT = 49;

    public AdminMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.ADMIN;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("admin", plugin.messages().get("admin-title"));
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        if (!player.hasPermission("aurum.admin")) {
            nav(holder, "back", BACK_SLOT, Material.ARROW, plugin.messages().get("back"));
            return;
        }
        int online = plugin.getServer().getOnlinePlayers().size();
        int cached = plugin.profiles().all().size();
        set(holder, 4, ItemBuilder.of(Material.BEACON)
                .name(plugin.messages().get("admin-title"))
                .lore(plugin.messages().get("admin-online", "&7Online: &f") + online,
                        plugin.messages().get("admin-cached", "&7Cached: &f") + cached,
                        plugin.messages().get("admin-economy", "&7Economy: &f") + plugin.economy().modeName())
                .build());

        TopService tops = plugin.tops();
        if (tops != null) {
            set(holder, 19, board(Material.GOLD_INGOT, plugin.messages().get("top-type-money"), tops.money(), true, player));
            set(holder, 25, board(Material.EMERALD, plugin.messages().get("top-type-points"), tops.points(), false, player));
        }
        PlayerProfile self = plugin.profiles().get(player.getUniqueId());
        if (self != null) {
            set(holder, 22, ItemBuilder.of(Material.PLAYER_HEAD)
                    .name("&f" + self.name())
                    .lore(placeholders(player, java.util.Arrays.asList(
                            plugin.messages().get("admin-points", "&7Aurum: &e") + Math.round(self.points()),
                            plugin.messages().get("admin-sold", "&7Sold: &e") + plugin.economy().format(self.moneyTotal()),
                            plugin.messages().get("admin-ulta", "&7Ulta: &e") + Math.round(self.ultaPoints()))))
                    .build());
        }
        nav(holder, "back", BACK_SLOT, Material.ARROW, plugin.messages().get("back"));
    }

    private org.bukkit.inventory.ItemStack board(Material material, String title, List<TopService.Entry> entries,
                                                 boolean money, Player player) {
        List<String> lore = new ArrayList<String>();
        int position = 1;
        for (TopService.Entry entry : entries) {
            String amount = money ? plugin.economy().format(entry.amount())
                    : com.xuvigan.aurumbuyer.util.Numbers.round(entry.amount());
            lore.add(plugin.messages().format("top-line",
                    "position", String.valueOf(position),
                    "name", entry.name(),
                    "amount", amount));
            position++;
        }
        if (lore.isEmpty()) {
            lore.add(plugin.messages().get("top-empty"));
        }
        return ItemBuilder.of(material).name("&6" + title).lore(placeholders(player, lore)).build();
    }
}
