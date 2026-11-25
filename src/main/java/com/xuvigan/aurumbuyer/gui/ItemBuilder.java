package com.xuvigan.aurumbuyer.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.xuvigan.aurumbuyer.util.Text;

/** Small item factory (1.16 safe: no Adventure, no item components). */
public final class ItemBuilder {

    @SuppressWarnings("deprecation")
    private static final Material FALLBACK = Material.matchMaterial("GRAY_STAINED_GLASS_PANE") == null
            ? Material.STONE : Material.matchMaterial("GRAY_STAINED_GLASS_PANE");

    private final ItemStack stack;

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material == null ? FALLBACK : material, 1));
    }

    public static ItemBuilder of(Material material, int amount) {
        int safe = Math.max(1, Math.min(64, amount));
        return new ItemBuilder(new ItemStack(material == null ? FALLBACK : material, safe));
    }

    public static ItemBuilder filler(Material material) {
        return of(material == null ? FALLBACK : material);
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = editableMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name == null ? " " : name));
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(java.util.List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return this;
        }
        ItemMeta meta = editableMeta();
        if (meta != null) {
            meta.setLore(Text.color(lore));
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (lines == null || lines.length == 0) {
            return this;
        }
        java.util.List<String> list = new java.util.ArrayList<String>();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                list.add(lines[i]);
            }
        }
        return lore(list);
    }


    public ItemBuilder customModelData(int modelData) {
        if (modelData <= 0) {
            return this;
        }
        ItemMeta meta = editableMeta();
        if (meta != null) {
            try {
                meta.setCustomModelData(Integer.valueOf(modelData));
                stack.setItemMeta(meta);
            } catch (Throwable ignored) {
                // pre-1.14 server: ignore
            }
        }
        return this;
    }

    /**
     * Glow without client-visible enchant text. Enchantment field names changed
     * in 1.20.5, so the constant is looked up reflectively and the item simply
     * stays unenchanted when nothing matches.
     */
    public ItemBuilder glow(boolean glow) {
        if (!glow) {
            return this;
        }
        org.bukkit.enchantments.Enchantment enchantment = resolveEnchantment();
        if (enchantment == null) {
            return this;
        }
        ItemMeta meta = editableMeta();
        if (meta == null) {
            return this;
        }
        try {
            meta.addEnchant(enchantment, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        } catch (Throwable ignored) {
            // leave the item plain
        }
        return this;
    }

    private static org.bukkit.enchantments.Enchantment resolveEnchantment() {
        String[] names = {"DURABILITY", "UNBREAKING"};
        for (int i = 0; i < names.length; i++) {
            try {
                Object value = org.bukkit.enchantments.Enchantment.class.getField(names[i]).get(null);
                if (value instanceof org.bukkit.enchantments.Enchantment) {
                    return (org.bukkit.enchantments.Enchantment) value;
                }
            } catch (Throwable ignored) {
                // try the next constant
            }
        }
        return null;
    }

    public ItemStack build() {
        return stack;
    }

    private ItemMeta editableMeta() {
        try {
            return stack.getItemMeta();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
