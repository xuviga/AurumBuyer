package com.xuvigan.aurumbuyer.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import com.xuvigan.aurumbuyer.AurumBuyer;

/**
 * Routes every click on an AurumBuyer inventory. Identification is done by the
 * {@link GuiHolder} instance stored in the inventory (event.getView()), never
 * by its title, so renamed titles can never leak items.
 */
public final class InventoryListener implements Listener {

    private final AurumBuyer plugin;

    public InventoryListener(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        GuiHolder top = holderOf(event.getView().getTopInventory());
        if (top == null) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        try {
            plugin.gui().handleClick(player, top, slot, event.getClick());
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[AurumBuyer] Menu click failed: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (holderOf(event.getView().getTopInventory()) == null) {
            return;
        }
        event.setCancelled(true);
    }

    static GuiHolder holderOf(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        try {
            org.bukkit.inventory.InventoryHolder holder = inventory.getHolder();
            return holder instanceof GuiHolder ? (GuiHolder) holder : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
