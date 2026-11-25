package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.config.ShopConfig;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;
import com.xuvigan.aurumbuyer.util.Numbers;

/**
 * Aurum shop: items are paid with Aurum points and get more expensive per
 * purchase. When the catalogue does not fit into one page the items are laid
 * out sequentially and previous/next buttons are added.
 */
public final class ShopMenu extends AbstractMenu {

    private static final Comparator<ShopConfig.ShopItem> ORDER = new Comparator<ShopConfig.ShopItem>() {
        @Override
        public int compare(ShopConfig.ShopItem left, ShopConfig.ShopItem right) {
            int bySlot = Integer.compare(left.slot(), right.slot());
            return bySlot != 0 ? bySlot : left.id().compareTo(right.id());
        }
    };

    public ShopMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.SHOP;
    }

    @Override
    protected int size() {
        int configured = plugin.config().menuSlots("shop", 54);
        return Math.max(9, Math.min(54, (configured / 9) * 9));
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("shop", plugin.messages().get("shop-title"));
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        ShopConfig shop = plugin.shop();
        int pageSize = Math.max(1, size() - 9);

        List<ShopConfig.ShopItem> items = new ArrayList<ShopConfig.ShopItem>();
        if (shop != null) {
            for (ShopConfig.ShopItem item : shop.items()) {
                if (item != null) {
                    items.add(item);
                }
            }
        }
        Collections.sort(items, ORDER);

        int pages = Math.max(1, (items.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(pages, holder.page()));
        holder.page(page);

        if (items.size() <= pageSize) {
            // configured slots are honoured while everything fits on one page
            boolean[] used = new boolean[Math.max(size(), pageSize)];
            int fallback = 0;
            for (ShopConfig.ShopItem item : items) {
                int slot = item.slot();
                if (slot < 0 || slot >= pageSize || used[slot]) {
                    while (fallback < pageSize && used[fallback]) {
                        fallback++;
                    }
                    slot = Math.min(pageSize - 1, fallback);
                }
                used[slot] = true;
                place(holder, slot, item, profile, player);
            }
        } else {
            int start = (page - 1) * pageSize;
            for (int position = 0; position < pageSize; position++) {
                int index = start + position;
                if (index >= items.size()) {
                    break;
                }
                place(holder, position, items.get(index), profile, player);
            }
        }

        if (page > 1) {
            nav(holder, "previous", size() - 9, Material.ARROW, plugin.messages().get("previous"));
        }
        nav(holder, "back", size() - 5, Material.ARROW, plugin.messages().get("back"));
        if (page < pages) {
            nav(holder, "next", size() - 1, Material.ARROW, plugin.messages().get("next"));
        }
    }

    private void place(GuiHolder holder, int slot, ShopConfig.ShopItem item, PlayerProfile profile, Player player) {
        set(holder, slot, build(item, profile, player));
        holder.action(slot, "SHOP:" + item.id());
    }

    private org.bukkit.inventory.ItemStack build(ShopConfig.ShopItem item, PlayerProfile profile, Player player) {
        int bought = profile == null ? 0 : profile.purchases(item.id());
        double cost = item.cost(bought);
        long globalBought = plugin.limits() == null ? 0L : plugin.limits().consumed("SHOP:" + item.id());

        boolean permitted = item.permission().isEmpty()
                || (player != null && player.hasPermission(item.permission()));

        List<String> lore = new ArrayList<String>();
        if (item.lore() != null) {
            for (String line : item.lore()) {
                // строки о правах показываем только тем, у кого права нет
                if (line.contains("%permission%") && permitted) {
                    continue;
                }
                lore.add(line
                        .replace("%cost%", plugin.economy().format(cost))
                        .replace("%bought%", String.valueOf(bought))
                        .replace("%personal_bought%", String.valueOf(bought))
                        .replace("%personal_limit%", item.personalLimit() > 0 ? String.valueOf(item.personalLimit()) : "-")
                        .replace("%global_bought%", String.valueOf(globalBought))
                        .replace("%global_limit%", item.globalLimit() > 0 ? String.valueOf(item.globalLimit()) : "-")
                        .replace("%display%", item.display())
                        .replace("%permission%", item.permission())
                        .replace("%points%", profile == null ? "0" : Numbers.round(profile.points()))
                        .replace("{points}", plugin.messages().pointsName()));
            }
        }
        lore.add("");
        lore.add(plugin.messages().get("buy-hint"));
        lore = placeholders(player, lore);
        boolean glow = item.glow() || (profile != null && profile.points() + 0.0000001D >= cost);
        return ItemBuilder.of(item.material(), item.amount())
                .name(placeholders(player, item.display()))
                .lore(lore)
                .customModelData(item.customModelData())
                .glow(glow)
                .build();
    }
}
