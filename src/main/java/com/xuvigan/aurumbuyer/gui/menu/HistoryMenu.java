package com.xuvigan.aurumbuyer.gui.menu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.data.HistoryEntry;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;
import com.xuvigan.aurumbuyer.util.Numbers;
import com.xuvigan.aurumbuyer.util.TimeUtil;

/** Paged sale history (45 entries + 9 navigation slots). */
public final class HistoryMenu extends AbstractMenu {

    private static final int PREVIOUS_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    public HistoryMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.HISTORY;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("history", plugin.messages().get("history-title")) + " " + Math.max(1, page);
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        int perPage = Math.max(1, Math.min(45, plugin.config().historyEntriesPerPage()));
        List<HistoryEntry> entries = profile == null ? new ArrayList<HistoryEntry>() : new ArrayList<HistoryEntry>(profile.history());
        int pages = Math.max(1, (entries.size() + perPage - 1) / perPage);
        int page = Math.max(1, Math.min(pages, holder.page()));
        holder.page(page);

        if (entries.isEmpty()) {
            set(holder, 22, ItemBuilder.of(Material.COBWEB)
                    .name(plugin.messages().get("history-empty")).build());
        } else {
            int start = (page - 1) * perPage;
            int end = Math.min(entries.size(), start + perPage);
            int slot = 0;
            for (int index = start; index < end && slot < perPage; index++, slot++) {
                HistoryEntry entry = entries.get(index);
                Material material = Material.matchMaterial(entry.itemId());
                if (material == null || material == Material.AIR) {
                    material = Material.PAPER;
                }
                String when = plugin.messages().get("history-time-format", "dd.MM HH:mm");
                String time;
                try {
                    time = new SimpleDateFormat(when.trim().isEmpty() ? "dd.MM HH:mm" : when.trim())
                            .format(new Date(entry.time()));
                } catch (IllegalArgumentException exception) {
                    time = TimeUtil.clock(entry.time());
                }
                List<String> lore = new ArrayList<String>();
                lore.add(plugin.messages().format("history-entry",
                        "time", time,
                        "item", prettyName(entry.itemId()),
                        "amount", String.valueOf(entry.amount()),
                        "money", plugin.economy().format(entry.money())));
                lore.add(plugin.messages().format("points-line", "points", Numbers.round(entry.points())));
                holder.action(slot, "HISTORY_ENTRY");
                set(holder, slot, ItemBuilder.of(material).amount(1).name("&f" + prettyName(entry.itemId()))
                        .lore(placeholders(player, lore)).build());
            }
        }

        if (page > 1) {
            nav(holder, "previous", PREVIOUS_SLOT, Material.ARROW, plugin.messages().get("previous"));
        }
        nav(holder, "back", BACK_SLOT, Material.BARRIER, plugin.messages().get("back"));
        if (page < pages) {
            nav(holder, "next", NEXT_SLOT, Material.ARROW, plugin.messages().get("next"));
        }
    }

    private static String prettyName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "?";
        }
        String lower = raw.toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
