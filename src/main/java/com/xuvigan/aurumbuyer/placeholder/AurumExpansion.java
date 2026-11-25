package com.xuvigan.aurumbuyer.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion (identifier {@code aurum}).
 *
 * <p>PlaceholderAPI is a compile time dependency of this class only; the class
 * is never loaded unless PlaceholderAPI is installed ({@link ExpansionBridge}
 * links it reflectively). No {@code @Override} annotations are used because the
 * supertype is not on the plugin classpath - the method signatures follow
 * PlaceholderExpansion 2.x exactly.</p>
 */
public final class AurumExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final AurumBuyerProvider provider;

    public AurumExpansion(AurumBuyerProvider provider) {
        this.provider = provider;
    }

    public String getIdentifier() {
        return "aurum";
    }

    public String getAuthor() {
        return "XuViGaN";
    }

    public String getVersion() {
        return provider.version();
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        return provider.resolve(player, PlaceholderService.PREFIX + params);
    }

    public String onRequest(OfflinePlayer player, String params) {
        Player online = player == null ? null : player.getPlayer();
        return onPlaceholderRequest(online, params);
    }
}
