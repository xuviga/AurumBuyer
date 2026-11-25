package com.xuvigan.aurumbuyer.util;

import java.util.Locale;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Guarded sound resolution: unknown names are silently ignored. */
public final class SoundUtil {

    private SoundUtil() {
    }

    public static Sound resolve(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toUpperCase(Locale.ENGLISH).replace(' ', '_').replace('-', '_').replace('.', '_');
        if (key.isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void play(Player player, String name) {
        play(player, name, 1.0F, 1.0F);
    }

    public static void play(Player player, String name, float volume, float pitch) {
        if (player == null) {
            return;
        }
        Sound sound = resolve(name);
        if (sound == null) {
            return;
        }
        play(player, sound, volume, pitch);
    }

    public static void play(Player player, Sound sound) {
        play(player, sound, 1.0F, 1.0F);
    }

    public static void play(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Throwable ignored) {
            // never break gameplay because of a sound name
        }
    }
}