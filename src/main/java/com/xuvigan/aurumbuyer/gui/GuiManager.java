package com.xuvigan.aurumbuyer.gui;

import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.config.ShopConfig;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.gui.menu.AdminMenu;
import com.xuvigan.aurumbuyer.gui.menu.BoosterMenu;
import com.xuvigan.aurumbuyer.gui.menu.HistoryMenu;
import com.xuvigan.aurumbuyer.gui.menu.LimitsMenu;
import com.xuvigan.aurumbuyer.gui.menu.MainMenu;
import com.xuvigan.aurumbuyer.gui.menu.SettingsMenu;
import com.xuvigan.aurumbuyer.gui.menu.ShopMenu;
import com.xuvigan.aurumbuyer.util.Numbers;
import com.xuvigan.aurumbuyer.util.SoundUtil;

/** Opens every menu and dispatches clicks by holder identity + action key. */
public final class GuiManager {

    private final AurumBuyer plugin;

    public GuiManager(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------- menus

    public void openMain(Player player) {
        new MainMenu(plugin).open(player, 1);
        sound(player, "click");
    }

    public void openShop(Player player, int page) {
        if (!plugin.config().shopEnabled()) {
            return;
        }
        if (plugin.config().shopRequirePermission() && !player.hasPermission("aurum.shop")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        new ShopMenu(plugin).open(player, page);
        sound(player, "click");
    }

    public void openHistory(Player player, int page) {
        new HistoryMenu(plugin).open(player, page);
        sound(player, "click");
    }

    public void openSettings(Player player) {
        new SettingsMenu(plugin).open(player, 1);
        sound(player, "click");
    }

    public void openBoosters(Player player) {
        new BoosterMenu(plugin).open(player, 1);
        sound(player, "click");
    }

    public void openLimits(Player player) {
        new LimitsMenu(plugin).open(player, 1);
        sound(player, "click");
    }

    public void openAdmin(Player player) {
        new AdminMenu(plugin).open(player, 1);
        sound(player, "click");
    }

    // -------------------------------------------------------------------- click

    /** Returns true when the click belongs to an AurumBuyer menu (caller must cancel it). */
    public boolean handleClick(Player player, GuiHolder holder, int slot, ClickType click) {
        if (holder == null || player == null) {
            return false;
        }
        MenuType type = holder.type();
        if (type == MenuType.ADMIN && !player.hasPermission("aurum.admin")) {
            return true;
        }
        String action = holder.action(slot);
        if (action == null) {
            return true;
        }
        String cmd = action.trim().toUpperCase(Locale.ENGLISH);
        if ("CLOSE".equals(cmd)) {
            player.closeInventory();
            sound(player, "click");
            return true;
        }
        if ("BACK".equals(cmd)) {
            openMain(player);
            return true;
        }
        if ("SUMMARY".equals(cmd)) {
            return true;
        }
        if ("PREVIOUS".equals(cmd)) {
            reopen(player, type, Math.max(1, holder.page() - 1));
            return true;
        }
        if ("NEXT".equals(cmd)) {
            reopen(player, type, holder.page() + 1);
            return true;
        }
        if ("SHOP".equals(cmd)) {
            openShop(player, 1);
            return true;
        }
        if ("HISTORY".equals(cmd)) {
            openHistory(player, 1);
            return true;
        }
        if ("SETTINGS".equals(cmd)) {
            openSettings(player);
            return true;
        }
        if ("BOOSTERS".equals(cmd)) {
            openBoosters(player);
            return true;
        }
        if ("LIMITS".equals(cmd)) {
            openLimits(player);
            return true;
        }
        if ("ULTA".equals(cmd)) {
            doubleUlta(player);
            refresh(player, type, holder.page());
            return true;
        }
        if ("AUTO_MODE".equals(cmd)) {
            toggleAutoMode(player);
            refresh(player, type, holder.page());
            return true;
        }
        if ("AUTO_MESSAGE".equals(cmd)) {
            PlayerProfile profile = profile(player);
            if (profile != null) {
                profile.autoMessage(!profile.autoMessage());
                plugin.profiles().save(profile);
                plugin.messages().send(player, profile.autoMessage() ? "auto-message-on" : "auto-message-off");
            }
            refresh(player, type, holder.page());
            return true;
        }
        if ("IGNORE_SLOTS".equals(cmd)) {
            PlayerProfile profile = profile(player);
            if (profile != null) {
                profile.ignoreSlots(!profile.ignoreSlots());
                plugin.profiles().save(profile);
            }
            refresh(player, type, holder.page());
            return true;
        }
        if (action.length() > 5 && action.substring(0, 5).equalsIgnoreCase("SHOP:")) {
            buyShopItem(player, action.substring(5), click != null && click.isShiftClick());
            refresh(player, type, holder.page());
            return true;
        }
        if (action.length() > 8 && action.substring(0, 8).equalsIgnoreCase("BOOSTER:")) {
            buyBooster(player, action.substring(8));
            refresh(player, type, holder.page());
            return true;
        }
        return true;
    }

    private void reopen(Player player, MenuType type, int page) {
        if (type == MenuType.SHOP) {
            openShop(player, page);
        } else if (type == MenuType.HISTORY) {
            openHistory(player, page);
        } else if (type == MenuType.BOOSTERS) {
            openBoosters(player);
        } else if (type == MenuType.LIMITS) {
            openLimits(player);
        } else if (type == MenuType.SETTINGS) {
            openSettings(player);
        } else if (type == MenuType.ADMIN) {
            openAdmin(player);
        } else {
            openMain(player);
        }
    }

    private void refresh(Player player, MenuType type, int page) {
        if (type == MenuType.MAIN) {
            openMain(player);
        } else if (type == MenuType.SETTINGS) {
            openSettings(player);
        } else if (type == MenuType.SHOP) {
            openShop(player, page);
        } else if (type == MenuType.BOOSTERS) {
            openBoosters(player);
        } else if (type == MenuType.HISTORY) {
            openHistory(player, page);
        } else if (type == MenuType.LIMITS) {
            openLimits(player);
        } else if (type == MenuType.ADMIN) {
            openAdmin(player);
        }
    }

    // ------------------------------------------------------------------ actions

    private void doubleUlta(Player player) {
        if (!player.hasPermission("aurum.boosters")) {
            plugin.messages().send(player, "no-permission");
            sound(player, "error");
            return;
        }
        PlayerProfile profile = profile(player);
        if (profile == null) {
            return;
        }
        if (plugin.boosters().ultaCooldownLeft(profile) > 0L) {
            plugin.messages().send(player, "booster-ulta-cooldown",
                    "time", com.xuvigan.aurumbuyer.util.TimeUtil.mmss(plugin.boosters().ultaCooldownLeft(profile)));
            sound(player, "error");
            return;
        }
        if (plugin.boosters().buyUltaBoost(player)) {
            plugin.messages().send(player, "booster-ulta-activated",
                    "percent", Numbers.round(plugin.config().ultaPercent()),
                    "minutes", String.valueOf(plugin.config().ultaMinutes()));
            sound(player, "success");
        } else {
            plugin.messages().send(player, "booster-ulta-no-points");
            sound(player, "error");
        }
    }

    private void toggleAutoMode(Player player) {
        PlayerProfile profile = profile(player);
        if (profile == null) {
            return;
        }
        if (plugin.config().autoSellRequirePermission() && !player.hasPermission("aurum.auto")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        AutoSellMode next = profile.autoMode().next();
        if (next == AutoSellMode.ENABLED && !plugin.config().autoSellEnabled()) {
            next = AutoSellMode.DISABLED;
        }
        profile.autoMode(next);
        plugin.profiles().save(profile);
        if (next == AutoSellMode.ENABLED) {
            plugin.messages().send(player, "auto-enabled");
        } else if (next == AutoSellMode.UNTIL_QUIT) {
            plugin.messages().send(player, "auto-until-quit");
        } else {
            plugin.messages().send(player, "auto-disabled");
        }
    }

    private void buyBooster(Player player, String category) {
        if (!player.hasPermission("aurum.boosters")) {
            plugin.messages().send(player, "no-permission");
            sound(player, "error");
            return;
        }
        PlayerProfile profile = profile(player);
        if (profile == null) {
            return;
        }
        double price = plugin.boosters().nextCost(profile, category);
        com.xuvigan.aurumbuyer.booster.BoosterService.BuyResult result = plugin.boosters().buy(player, category);
        if (result == com.xuvigan.aurumbuyer.booster.BoosterService.BuyResult.OK) {
            plugin.messages().send(player, "booster-bought",
                    "category", plugin.boosters().display(category),
                    "level", String.valueOf(plugin.boosters().level(profile, category)),
                    "cost", Numbers.round(price));
            sound(player, "success");
        } else if (result == com.xuvigan.aurumbuyer.booster.BoosterService.BuyResult.NO_MONEY) {
            plugin.messages().send(player, "booster-no-points");
            sound(player, "error");
        } else {
            plugin.messages().send(player, "booster-max");
            sound(player, "error");
        }
    }

    private void buyShopItem(Player player, String itemId, boolean shiftClick) {
        ShopConfig shop = plugin.shop();
        if (shop == null) {
            return;
        }
        ShopConfig.ShopItem item = shop.item(itemId);
        if (item == null) {
            return;
        }
        if (shop.requirePermission() && !player.hasPermission("aurum.shop")) {
            plugin.messages().send(player, "no-permission");
            sound(player, "error");
            return;
        }
        if (item.hasPermissionNode() && !player.hasPermission(item.permission())) {
            String custom = item.noPermissionMessage() == null || item.noPermissionMessage().isEmpty()
                    ? plugin.messages().get("shop-no-permission") : item.noPermissionMessage();
            plugin.messages().sendRaw(player, custom);
            sound(player, "error");
            return;
        }
        PlayerProfile profile = profile(player);
        if (profile == null) {
            return;
        }
        int attempts = shiftClick ? Math.max(1, shop.maxPurchasesPerClick()) : 1;
        String limitKey = "SHOP:" + item.id();
        double spent = 0.0D;
        int boughtNow = 0;
        String failure = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int bought = profile.purchases(item.id());
            if (item.personalLimit() > 0 && bought >= item.personalLimit()) {
                failure = "shop-limit-personal";
                break;
            }
            long globalBought = plugin.limits().consumed(limitKey);
            if (item.globalLimit() > 0 && globalBought >= item.globalLimit()) {
                failure = "shop-limit-global";
                break;
            }
            if (!canReceive(player, item)) {
                failure = "inventory-full";
                break;
            }
            double cost = item.cost(bought);
            synchronized (profile) {
                if (!profile.takePoints(cost)) {
                    failure = "shop-no-points";
                    break;
                }
                profile.addPurchase(item.id(), 1);
            }
            if (item.globalLimit() > 0) {
                plugin.limits().consume(limitKey, 1L);
            }
            spent += cost;
            boughtNow++;
            dispatchCommands(item, player);
        }
        if (boughtNow <= 0) {
            if (failure != null) {
                sendShopFailure(player, item, failure);
            }
            sound(player, "error");
            return;
        }
        plugin.profiles().save(profile);
        String success = item.successMessage() == null || item.successMessage().isEmpty()
                ? plugin.messages().format("shop-bought",
                        "display", item.display(), "cost", plugin.economy().format(spent))
                : com.xuvigan.aurumbuyer.util.Text.formatPairs(item.successMessage(),
                        "display", item.display(), "cost", plugin.economy().format(spent));
        plugin.messages().sendRaw(player, success);
        sound(player, "success");
    }

    private void dispatchCommands(ShopConfig.ShopItem item, Player player) {
        for (String command : item.commands()) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            String line = command.replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), line);
        }
    }

    private void sendShopFailure(Player player, ShopConfig.ShopItem item, String key) {
        boolean limit = "shop-limit-personal".equals(key) || "shop-limit-global".equals(key);
        if (limit && item.limitMessage() != null && !item.limitMessage().isEmpty()) {
            plugin.messages().sendRaw(player, item.limitMessage());
            return;
        }
        plugin.messages().send(player, key);
    }

    /** Enough free room for the reward item? Items without a give command are not checked. */
    private boolean canReceive(Player player, ShopConfig.ShopItem item) {
        boolean givesItem = false;
        for (String command : item.commands()) {
            if (command != null && command.toLowerCase(java.util.Locale.ENGLISH).startsWith("give ")) {
                givesItem = true;
                break;
            }
        }
        if (!givesItem || item.material() == null || item.material() == org.bukkit.Material.AIR) {
            return true;
        }
        int needed = Math.max(1, item.amount());
        for (org.bukkit.inventory.ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() == org.bukkit.Material.AIR) {
                int free = Math.min(64, Math.max(1, stack == null ? 64 : stack.getType().getMaxStackSize()));
                needed -= Math.max(1, free);
            } else if (stack.getType() == item.material()) {
                int room = stack.getType().getMaxStackSize() - stack.getAmount();
                needed -= Math.max(0, room);
            }
            if (needed <= 0) {
                return true;
            }
        }
        return needed <= 0;
    }

    // ------------------------------------------------------------------ helpers

    private PlayerProfile profile(Player player) {
        return plugin.profiles().get(player.getUniqueId());
    }

    private void sound(Player player, String key) {
        if (plugin.config().soundEnabled()) {
            SoundUtil.play(player, plugin.config().sound(key));
        }
    }

    /** Kept for the listener: the inventory holder of a live inventory. */
    public static GuiHolder holderOf(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        org.bukkit.inventory.InventoryHolder holder = inventory.getHolder();
        return holder instanceof GuiHolder ? (GuiHolder) holder : null;
    }
}
