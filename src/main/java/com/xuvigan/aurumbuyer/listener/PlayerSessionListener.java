package com.xuvigan.aurumbuyer.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;

/**
 * Keeps profiles in sync with the player session: refreshes the stored name,
 * expires the "until quit" auto-sell mode and unloads the profile on quit.
 */
public final class PlayerSessionListener implements Listener {

    private final AurumBuyer plugin;

    public PlayerSessionListener(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.name(player.getName());
        // a value left over from a crash/restart must not resurrect auto-sell
        if (profile.autoMode() == AutoSellMode.UNTIL_QUIT) {
            profile.autoMode(AutoSellMode.DISABLED);
        }
        if (profile.dirty()) {
            plugin.profiles().save(profile);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        PlayerProfile profile = plugin.profiles().cached(id);
        if (profile == null) {
            return;
        }
        if (profile.autoMode() == AutoSellMode.UNTIL_QUIT) {
            profile.autoMode(AutoSellMode.DISABLED);
        }
        if (plugin.boosters() != null) {
            plugin.boosters().cleanup(profile);
        }
        plugin.profiles().saveBlocking(profile);
        plugin.profiles().unload(id);
    }
}
