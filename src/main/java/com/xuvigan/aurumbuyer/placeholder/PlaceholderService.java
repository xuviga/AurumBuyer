package com.xuvigan.aurumbuyer.placeholder;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.booster.BoosterCategory;
import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.engine.MultiplierService;
import com.xuvigan.aurumbuyer.util.Numbers;

/**
 * Resolves every {@code %aurum_*%} placeholder (SPEC.md section 10.10).
 * Unknown identifiers resolve to an empty string.
 */
public final class PlaceholderService {

    public static final String PREFIX = "aurum_";

    private final AurumBuyer plugin;

    public PlaceholderService(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    /** Replaces every {@code %aurum_...%} token found in the text. */
    public String apply(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        if (text.indexOf(PREFIX) < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 32);
        int index = 0;
        while (index < text.length()) {
            int open = text.indexOf('%', index);
            if (open < 0) {
                out.append(text.substring(index));
                break;
            }
            int close = text.indexOf('%', open + 1);
            if (close < 0) {
                out.append(text.substring(index));
                break;
            }
            out.append(text.substring(index, open));
            String identifier = text.substring(open + 1, close);
            if (identifier.toLowerCase(Locale.ENGLISH).startsWith(PREFIX)) {
                out.append(resolve(player, identifier));
            } else {
                out.append('%').append(identifier).append('%');
            }
            index = close + 1;
        }
        return out.toString();
    }

    /** Resolves an identifier that already lost its percent signs, e.g. {@code aurum_points}. */
    public String resolve(Player player, String identifier) {
        if (identifier == null) {
            return "";
        }
        String id = identifier.trim().toLowerCase(Locale.ENGLISH);
        int percent = id.indexOf('%');
        if (percent >= 0) {
            id = id.replace("%", "").trim();
        }
        if (!id.startsWith(PREFIX)) {
            return "";
        }
        String key = id.substring(PREFIX.length());
        if (key.isEmpty()) {
            return "";
        }
        if (key.startsWith("top_money_") || key.startsWith("top_points_")) {
            return top(key);
        }
        if (key.startsWith("sold_")) {
            PlayerProfile profile = profile(player);
            if (profile == null) {
                return "";
            }
            String item = key.substring("sold_".length());
            Material material = Material.matchMaterial(item.toUpperCase(Locale.ENGLISH));
            String itemId = material == null ? item.toUpperCase(Locale.ENGLISH) : material.name();
            return String.valueOf(profile.sold(itemId));
        }
        if (key.startsWith("booster_")) {
            return booster(player, key.substring("booster_".length()));
        }
        return simple(player, key);
    }

    // ------------------------------------------------------------------ simple

    private String simple(Player player, String key) {
        ConfigManager config = plugin.config();
        if ("auto_mode".equals(key)) {
            PlayerProfile profile = profile(player);
            return profile == null ? "" : profile.autoMode().name();
        }
        if ("ulta_max".equals(key)) {
            return Numbers.round(config.ultaMaxPoints());
        }
        PlayerProfile profile = profile(player);
        if (profile == null) {
            return "";
        }
        if ("points".equals(key)) {
            return Numbers.round(profile.points());
        }
        if ("total_points".equals(key)) {
            return Numbers.round(profile.pointsTotal());
        }
        if ("total_money".equals(key)) {
            return plugin.economy().format(profile.moneyTotal());
        }
        if ("ulta".equals(key)) {
            return Numbers.round(profile.ultaPoints());
        }
        if ("multiplier".equals(key)) {
            MultiplierService multipliers = plugin.multipliers();
            return Numbers.format(multipliers == null ? 1.0D : multipliers.multiplier(profile), "0.00");
        }
        if ("multiplier_percent".equals(key)) {
            MultiplierService multipliers = plugin.multipliers();
            return Numbers.round(multipliers == null ? 0.0D : multipliers.percent(profile));
        }
        if ("name".equals(key)) {
            return profile.name();
        }
        if ("balance".equals(key)) {
            return plugin.economy().format(plugin.economy().balance(player));
        }
        return "";
    }

    // -------------------------------------------------------------------- tops

    private String top(String key) {
        if (plugin.tops() == null) {
            return "";
        }
        boolean money = key.startsWith("top_money_");
        String rest = key.substring(money ? "top_money_".length() : "top_points_".length());
        String position = rest;
        String field = "name";
        int underscore = rest.indexOf('_');
        if (underscore > 0) {
            position = rest.substring(0, underscore);
            field = rest.substring(underscore + 1);
        }
        int index = Numbers.parseInt(position, 0);
        TopService.Entry entry = money ? plugin.tops().money(index) : plugin.tops().points(index);
        if (entry == null) {
            return "";
        }
        if ("name".equals(field)) {
            return entry.name();
        }
        if ("amount".equals(field) || position.equals(rest)) {
            return money ? plugin.economy().format(entry.amount()) : Numbers.round(entry.amount());
        }
        return "";
    }

    // ---------------------------------------------------------------- boosters

    private String booster(Player player, String rest) {
        if (rest.isEmpty()) {
            return "";
        }
        if ("spent_total".equals(rest)) {
            PlayerProfile profile = profile(player);
            return profile == null ? "" : Numbers.round(profile.boosterSpentTotal());
        }
        int split = rest.indexOf('_');
        if (split <= 0) {
            return "";
        }
        String cat = rest.substring(0, split);
        String parameter = rest.substring(split + 1);
        if (plugin.boosters() == null || !plugin.boosters().exists(cat)) {
            return "";
        }
        PlayerProfile profile = profile(player);
        if (profile == null) {
            return "";
        }
        int level = plugin.boosters().level(profile, cat);
        BoosterCategory category = plugin.boosters().category(cat);
        if ("cost".equals(parameter)) {
            return Numbers.round(plugin.boosters().nextCost(profile, cat));
        }
        if ("max_level".equals(parameter)) {
            return String.valueOf(plugin.boosters().maxLevel(cat));
        }
        if ("next_bonus".equals(parameter)) {
            double next = category == null ? 0.0D : category.percentAt(Math.min(level + 1, category.maxLevel()));
            return Numbers.round(next - (category == null ? 0.0D : category.percentAt(level)));
        }
        if ("next_percent".equals(parameter)) {
            return Numbers.round(category == null ? 0.0D : category.percentAt(Math.min(level + 1, category.maxLevel())));
        }
        if ("percent".equals(parameter)) {
            return Numbers.round(plugin.boosters().percent(profile, cat));
        }
        if ("permanent_percent".equals(parameter)) {
            return Numbers.round(plugin.boosters().permanentPercent(profile, cat));
        }
        if ("temp_percent".equals(parameter)) {
            return Numbers.round(plugin.boosters().tempPercent(profile, cat));
        }
        if ("temp_seconds".equals(parameter)) {
            return String.valueOf(plugin.boosters().tempSecondsLeft(profile, cat));
        }
        if ("multiplier".equals(parameter)) {
            return Numbers.format(plugin.boosters().multiplier(profile, cat), "0.00");
        }
        if ("level".equals(parameter)) {
            return String.valueOf(level);
        }
        if ("is_max".equals(parameter)) {
            return plugin.boosters().isMax(profile, cat) ? "true" : "false";
        }
        if ("spent".equals(parameter)) {
            return Numbers.round(profile.boosterSpent(cat));
        }
        if ("display".equals(parameter)) {
            return plugin.boosters().display(cat);
        }
        if (parameter.startsWith("cost_")) {
            return Numbers.round(plugin.boosters().cost(cat, Numbers.parseInt(parameter.substring(5), 0)));
        }
        if (parameter.startsWith("percent_")) {
            return Numbers.round(category == null ? 0.0D
                    : category.percentAt(Numbers.parseInt(parameter.substring(8), 0)));
        }
        if ("mode".equals(parameter)) {
            return profile.autoMode() == AutoSellMode.DISABLED ? "off" : profile.autoMode().name().toLowerCase(Locale.ENGLISH);
        }
        return "";
    }

    // ----------------------------------------------------------------- helpers

    private PlayerProfile profile(Player player) {
        if (player == null) {
            if (plugin.getServer().getOnlinePlayers().size() == 1) {
                Player only = plugin.getServer().getOnlinePlayers().iterator().next();
                return plugin.profiles().cached(only.getUniqueId());
            }
            return null;
        }
        return plugin.profiles().get(player.getUniqueId());
    }
}
