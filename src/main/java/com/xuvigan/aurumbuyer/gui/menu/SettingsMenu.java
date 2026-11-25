package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;

/** Player settings: auto-sell mode, notifications, ignored slot, fast sell. */
public final class SettingsMenu extends AbstractMenu {

    public SettingsMenu(AurumBuyer plugin) {
        super(plugin);
    }

    @Override
    public MenuType type() {
        return MenuType.SETTINGS;
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected String title(int page) {
        return plugin.config().menuTitle("settings", plugin.messages().get("settings-title"));
    }

    @Override
    protected void render(Player player, GuiHolder holder) {
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        if (profile == null) {
            return;
        }
        List<String> hint = new ArrayList<String>();
        hint.add(plugin.messages().get("settings-hint"));

        set(holder, 10, toggle(Material.HOPPER, plugin.messages().get("settings-auto-mode"),
                modeName(profile.autoMode()), hint));
        holder.action(10, "AUTO_MODE");

        set(holder, 12, toggle(Material.BELL, plugin.messages().get("settings-auto-message"),
                onOff(profile.autoMessage()), hint));
        holder.action(12, "AUTO_MESSAGE");

        set(holder, 14, toggle(Material.IRON_BOOTS, plugin.messages().get("settings-ignore-slots"),
                onOff(profile.ignoreSlots()), hint));
        holder.action(14, "IGNORE_SLOTS");

        set(holder, 22, ItemBuilder.of(Material.ARROW).name(plugin.messages().get("back")).build());
        holder.action(22, "BACK");
    }

    private org.bukkit.inventory.ItemStack toggle(Material material, String name, String state, List<String> hint) {
        ItemBuilder builder = ItemBuilder.of(material).name(name);
        List<String> lore = new ArrayList<String>();
        lore.add("&7" + state);
        lore.addAll(hint);
        builder.lore(lore);
        return builder.build();
    }

    private String onOff(boolean value) {
        return value ? plugin.messages().get("state-on") : plugin.messages().get("state-off");
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
}
