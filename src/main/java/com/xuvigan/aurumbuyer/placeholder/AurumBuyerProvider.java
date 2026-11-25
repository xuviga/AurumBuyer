package com.xuvigan.aurumbuyer.placeholder;

import org.bukkit.entity.Player;

/**
 * Decouples the (reflection-loaded) PlaceholderAPI expansion from the plugin
 * class so that no PlaceholderAPI type is ever touched when the plugin is
 * missing.
 */
public interface AurumBuyerProvider {

    /** Plugin version reported to PlaceholderAPI. */
    String version();

    /** Resolves one identifier, with or without its percent signs. */
    String resolve(Player player, String identifier);
}
