package com.xuvigan.aurumbuyer.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.xuvigan.aurumbuyer.util.Text;

/** Typed accessor for config.yml (sections 5 and 10.8 of SPEC.md). */
public final class ConfigManager {

    public enum EconomyMode {
        VAULT,
        INTERNAL
    }

    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public String string(String path, String def) {
        String value = cfg == null ? null : cfg.getString(path);
        return value == null ? def : value;
    }

    public String text(String path, String def) {
        return Text.color(string(path, def));
    }

    public int integer(String path, int def) {
        return cfg.getInt(path, def);
    }

    public long longValue(String path, long def) {
        return cfg.getLong(path, def);
    }

    public double number(String path, double def) {
        return cfg.getDouble(path, def);
    }

    public boolean flag(String path, boolean def) {
        return cfg.getBoolean(path, def);
    }

    public List<String> stringList(String path) {
        List<String> list = cfg == null ? null : cfg.getStringList(path);
        return list == null ? new ArrayList<String>() : new ArrayList<String>(list);
    }

    public List<String> colorList(String path) {
        return Text.color(stringList(path));
    }

    public Material material(String path, Material def) {
        String value = string(path, null);
        if (value == null) {
            return def;
        }
        Material match = Material.matchMaterial(value.trim().toUpperCase(Locale.ENGLISH));
        return match == null ? def : match;
    }

    public int customModelData(String path) {
        return cfg.getInt(path + ".custom-model-data", 0);
    }

    // ------------------------------------------------------------------ general

    public int saveIntervalSeconds() {
        return Math.max(20, integer("general.save-interval-seconds", 300));
    }

    public int historySize() {
        return Math.max(1, integer("general.history-size", 100));
    }

    public boolean soundEnabled() {
        return flag("general.sound-enabled", true);
    }

    public String sound(String key) {
        return string("general.sounds." + key, "");
    }

    // ------------------------------------------------------------------ economy

    public EconomyMode economyMode() {
        String mode = string("economy.mode", "VAULT");
        return "INTERNAL".equalsIgnoreCase(mode.trim()) ? EconomyMode.INTERNAL : EconomyMode.VAULT;
    }

    public boolean vaultRequested() {
        return economyMode() == EconomyMode.VAULT;
    }

    public String currencyFormat() {
        return string("economy.currency-format", "#,##0.00");
    }

    public String currencyName() {
        return string("economy.currency-name", "&6Aurum");
    }

    public double internalStartBalance() {
        return number("economy.internal.start-balance", 0.0D);
    }

    // --------------------------------------------------------------------- sell

    public double sellPointsPercent() {
        return number("sell.points-percent", 5.0D);
    }

    // --------------------------------------------------------------- multiplier

    public boolean multiplierEnabled() {
        return flag("multiplier.enabled", true);
    }

    public double multiplierStep() {
        return number("multiplier.step", 100000.0D);
    }

    public double multiplierPerStepPercent() {
        return number("multiplier.per-step-percent", 2.0D);
    }

    public double multiplierMaxPercent() {
        return number("multiplier.max-percent", 100.0D);
    }

    public List<Double> multiplierTiers() {
        List<Double> out = new ArrayList<Double>();
        List<?> raw = cfg == null ? null : cfg.getList("multiplier.tiers");
        if (raw == null) {
            return out;
        }
        for (Object entry : raw) {
            if (entry instanceof Number) {
                out.add(Double.valueOf(((Number) entry).doubleValue()));
            } else if (entry != null) {
                try {
                    out.add(Double.valueOf(Double.parseDouble(String.valueOf(entry).trim())));
                } catch (NumberFormatException ignored) {
                    // skip malformed tier
                }
            }
        }
        return out;
    }

    // ----------------------------------------------------------------- auto-sell

    public boolean autoSellEnabled() {
        return flag("auto-sell.enabled", true);
    }

    public int autoSellIntervalTicks() {
        return Math.max(20, integer("auto-sell.interval-ticks", 100));
    }

    public boolean autoSellRequirePermission() {
        return flag("auto-sell.require-permission", false);
    }

    public int autoSellMinItems() {
        return Math.max(1, integer("auto-sell.min-items", 1));
    }

    public int autoSellMaxItemsPerCycle() {
        return Math.max(1, integer("auto-sell.max-items-per-cycle", 2304));
    }

    public List<Integer> autoSellIgnoreSlots() {
        List<Integer> out = new ArrayList<Integer>();
        List<?> raw = cfg == null ? null : cfg.getList("auto-sell.ignore-slots");
        if (raw == null) {
            out.add(Integer.valueOf(0));
            return out;
        }
        for (Object entry : raw) {
            if (entry instanceof Number) {
                out.add(Integer.valueOf(((Number) entry).intValue()));
            } else if (entry != null) {
                try {
                    out.add(Integer.valueOf(Integer.parseInt(String.valueOf(entry).trim())));
                } catch (NumberFormatException ignored) {
                    // skip malformed slot
                }
            }
        }
        return out;
    }

    public String autoSellPermission() {
        return "aurum.auto";
    }

    // ------------------------------------------------------------------- limits

    public long globalResetHours() {
        return Math.max(1L, longValue("limits.global-reset-hours", 24L));
    }

    public long personalResetHours() {
        return Math.max(1L, longValue("limits.personal-reset-hours", 24L));
    }

    // --------------------------------------------------------------------- ulta

    public boolean ultaEnabled() {
        return flag("ulta.enabled", true);
    }

    public double ultaPointsPercent() {
        return number("ulta.points-percent", 5.0D);
    }

    public double ultaMaxPoints() {
        return number("ulta.max-points", 50000.0D);
    }

    public double ultaCost() {
        return number("ulta.cost", 5000.0D);
    }

    public double ultaPercent() {
        return number("ulta.percent", 25.0D);
    }

    public int ultaMinutes() {
        return Math.max(1, integer("ulta.minutes", 10));
    }

    public int ultaCooldownMinutes() {
        return Math.max(0, integer("ulta.cooldown-minutes", 30));
    }

    // --------------------------------------------------------------------- shop

    public boolean shopEnabled() {
        return flag("shop.enabled", true);
    }

    public double shopPriceIncreasePercent() {
        return number("shop.price-increase-percent", 8.0D);
    }

    public int shopMaxPurchasesPerClick() {
        return Math.max(1, integer("shop.max-purchases-per-click", 1));
    }

    public boolean shopRequirePermission() {
        return flag("shop.require-permission", false);
    }

    // ------------------------------------------------------------- placeholders

    public int topSize() {
        return Math.max(1, integer("placeholders.top-size", 10));
    }

    public int topRefreshSeconds() {
        return Math.max(10, integer("placeholders.refresh-seconds", 120));
    }

    // ---------------------------------------------------------------------- gui

    public Material fillerMaterial() {
        return material("gui.filler.material", Material.GRAY_STAINED_GLASS_PANE);
    }

    public String fillerName() {
        return text("gui.filler.name", " ");
    }

    public int fillerCustomModelData() {
        return integer("gui.filler.custom-model-data", 0);
    }

    public String menuTitle(String menu, String def) {
        return text("gui." + menu + ".title", def);
    }

    public int menuSlots(String menu, int def) {
        return integer("gui." + menu + ".slots", def);
    }

    public int historyEntriesPerPage() {
        return Math.max(1, integer("gui.history.entries-per-page", 45));
    }

    public Material menuItemMaterial(String menu, String key, Material def) {
        return material("gui." + menu + ".items." + key + ".material", def);
    }

    public int menuItemSlot(String menu, String key, int def) {
        return integer("gui." + menu + ".items." + key + ".slot", def);
    }

    public String menuItemName(String menu, String key, String def) {
        return text("gui." + menu + ".items." + key + ".name", def);
    }

    public int menuItemCustomModelData(String menu, String key) {
        return integer("gui." + menu + ".items." + key + ".custom-model-data", 0);
    }

    public boolean menuItemGlow(String menu, String key) {
        return flag("gui." + menu + ".items." + key + ".glow", false);
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}