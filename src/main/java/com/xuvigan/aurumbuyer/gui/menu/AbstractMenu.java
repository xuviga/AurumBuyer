package com.xuvigan.aurumbuyer.gui.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.gui.GuiHolder;
import com.xuvigan.aurumbuyer.gui.ItemBuilder;
import com.xuvigan.aurumbuyer.gui.MenuType;

/** Shared plumbing for every AurumBuyer menu. */
public abstract class AbstractMenu {

    protected final AurumBuyer plugin;

    protected AbstractMenu(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    public abstract MenuType type();

    /** Creates the inventory, fills it through {@link #render} and returns the holder. */
    public GuiHolder create(Player player, int page) {
        GuiHolder holder = new GuiHolder(type(), page);
        Inventory inventory = plugin.getServer().createInventory(holder, size(), title(page));
        holder.inventory(inventory);
        fill(holder);
        render(player, holder);
        return holder;
    }

    public GuiHolder open(Player player, int page) {
        GuiHolder holder = create(player, page);
        player.openInventory(holder.getInventory());
        return holder;
    }

    protected abstract int size();

    protected abstract String title(int page);

    protected abstract void render(Player player, GuiHolder holder);

    // ------------------------------------------------------------------ filler

    protected void fill(GuiHolder holder) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        Material material = plugin.config().fillerMaterial();
        String name = plugin.config().fillerName();
        int modelData = plugin.config().fillerCustomModelData();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, ItemBuilder.filler(material).name(name).customModelData(modelData).build());
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    protected void set(GuiHolder holder, int slot, ItemStack item) {
        Inventory inventory = holder.getInventory();
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
    }

    /** Navigation button (previous / next / back / close). */
    protected void nav(GuiHolder holder, String key, int slot, Material fallback, String nameFallback) {
        String name = plugin.messages().get(key + "-button-name", nameFallback);
        ItemStack item = ItemBuilder.of(fallback).name(name).build();
        set(holder, slot, item);
        holder.action(slot, key);
    }

    /** Expands {@code %aurum_*%} tokens in a menu line for the viewer. */
    protected String placeholders(Player player, String text) {
        if (text == null || text.isEmpty() || player == null || plugin.placeholders() == null) {
            return text == null ? "" : text;
        }
        return plugin.placeholders().apply(player, text);
    }

    protected List<String> placeholders(Player player, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines;
        }
        List<String> out = new ArrayList<String>(lines.size());
        for (String line : lines) {
            out.add(placeholders(player, line));
        }
        return out;
    }
}
