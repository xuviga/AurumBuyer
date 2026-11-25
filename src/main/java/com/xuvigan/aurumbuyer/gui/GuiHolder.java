package com.xuvigan.aurumbuyer.gui;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Inventory holder identifying an AurumBuyer menu. Carries the page number and
 * an optional subject (shop item id, booster category) so the click listener
 * never has to parse the inventory title.
 */
public final class GuiHolder implements InventoryHolder {

    private final MenuType type;
    private final Map<Integer, String> actions = new HashMap<Integer, String>();
    private Inventory inventory;
    private int page = 1;
    private String subject;

    public GuiHolder(MenuType type) {
        this.type = type == null ? MenuType.MAIN : type;
    }

    public GuiHolder(MenuType type, int page) {
        this(type);
        this.page = Math.max(1, page);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public MenuType type() {
        return type;
    }

    public MenuType getType() {
        return type;
    }

    public int page() {
        return page;
    }

    public void page(int page) {
        this.page = Math.max(1, page);
    }

    public String subject() {
        return subject;
    }

    public void subject(String subject) {
        this.subject = subject;
    }

    /** Binds a slot to an action key consumed by {@link InventoryListener}. */
    public GuiHolder action(int slot, String key) {
        if (key != null) {
            actions.put(Integer.valueOf(slot), key);
        }
        return this;
    }

    public String action(int slot) {
        return actions.get(Integer.valueOf(slot));
    }

    public Map<Integer, String> actions() {
        return actions;
    }

    @Override
    public String toString() {
        return "GuiHolder{" + type + ", page=" + page + (subject == null ? "" : ", subject=" + subject) + "}";
    }
}
