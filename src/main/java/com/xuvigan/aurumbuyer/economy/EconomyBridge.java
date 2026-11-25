package com.xuvigan.aurumbuyer.economy;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.data.ProfileManager;

/**
 * Economy abstraction. VAULT mode talks to Vault through reflection only, so
 * the plugin has no compile-time dependency; any failure silently falls back
 * to the plugin owned INTERNAL balance stored in the player profile.
 */
public final class EconomyBridge {

    public enum Mode {
        VAULT,
        INTERNAL
    }

    private final Plugin plugin;
    private final ProfileManager profiles;
    private String requestedMode;
    private String currencyFormat;

    private Mode mode = Mode.INTERNAL;
    private Object economy;
    private Method balanceMethod;
    private Method depositMethod;
    private Method withdrawMethod;
    private Method formatMethod;
    private boolean warned;

    public EconomyBridge(Plugin plugin, ProfileManager profiles, String requestedMode, String currencyFormat) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.requestedMode = requestedMode == null ? "VAULT" : requestedMode.trim();
        this.currencyFormat = currencyFormat == null || currencyFormat.trim().isEmpty() ? "#,##0.00" : currencyFormat.trim();
        setup();
    }

    /** Applies a new economy section from config.yml; call before {@link #setup()}. */
    public void configure(String mode, String format) {
        this.requestedMode = mode == null ? "VAULT" : mode.trim();
        this.currencyFormat = format == null || format.trim().isEmpty() ? "#,##0.00" : format.trim();
    }

    /** Re-detects Vault; safe to call on /aurum reload. */
    public final void setup() {
        this.mode = Mode.INTERNAL;
        this.economy = null;
        this.balanceMethod = null;
        this.depositMethod = null;
        this.withdrawMethod = null;
        this.formatMethod = null;
        this.warned = false;
        if (!"VAULT".equalsIgnoreCase(requestedMode)) {
            return;
        }
        try {
            Class<?> eco = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(eco);
            if (registration == null) {
                logFallback("Vault is not installed");
                return;
            }
            Object provider = registration.getClass().getMethod("getProvider").invoke(registration);
            if (provider == null) {
                logFallback("Vault provider is null");
                return;
            }
            Method balance = findMethod(eco, "getBalance", 1, OfflinePlayer.class);
            Method deposit = findMethod(eco, "depositPlayer", 2, OfflinePlayer.class);
            Method withdraw = findMethod(eco, "withdrawPlayer", 2, OfflinePlayer.class);
            if (balance == null || deposit == null || withdraw == null) {
                logFallback("Vault economy methods are missing");
                return;
            }
            this.balanceMethod = balance;
            this.depositMethod = deposit;
            this.withdrawMethod = withdraw;
            this.formatMethod = findFormat(eco);
            this.economy = provider;
            this.mode = Mode.VAULT;
            if (plugin != null) {
                plugin.getLogger().info("[AurumBuyer] Economy mode: VAULT");
            }
        } catch (Throwable throwable) {
            logFallback(throwable.getClass().getSimpleName() + (throwable.getMessage() == null ? "" : ": " + throwable.getMessage()));
            this.mode = Mode.INTERNAL;
            this.economy = null;
        }
    }

    private void logFallback(String reason) {
        if (warned || plugin == null) {
            return;
        }
        warned = true;
        plugin.getLogger().warning("[AurumBuyer] " + reason + " - using INTERNAL economy.");
    }

    private static Method findMethod(Class<?> eco, String name, int params, Class<?> firstType) {
        Method[] methods = eco.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!method.getName().equals(name)) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length != params) {
                continue;
            }
            if (types.length == 0 || !firstType.isAssignableFrom(types[0])) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static Method findFormat(Class<?> eco) {
        Method[] methods = eco.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!method.getName().equals("format")) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 1 && (types[0] == double.class || types[0] == Double.class)) {
                return method;
            }
        }
        return null;
    }

    public boolean vaultPresent() {
        return mode == Mode.VAULT && economy != null;
    }

    public Mode mode() {
        return mode;
    }

    public String modeName() {
        return mode.name();
    }

    public boolean internal() {
        return mode == Mode.INTERNAL;
    }

    public double balance(Player player) {
        if (player == null) {
            return 0.0D;
        }
        if (vaultPresent()) {
            try {
                Object result = balanceMethod.invoke(economy, player);
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            } catch (Throwable throwable) {
                switchToInternal("getBalance", throwable);
            }
        }
        PlayerProfile profile = profileOf(player);
        return profile == null ? 0.0D : profile.internalBalance();
    }

    public boolean deposit(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return false;
        }
        if (vaultPresent()) {
            try {
                Object response = depositMethod.invoke(economy, player, Double.valueOf(amount));
                return response == null || success(response);
            } catch (Throwable throwable) {
                switchToInternal("depositPlayer", throwable);
            }
        }
        PlayerProfile profile = profileOf(player);
        if (profile == null) {
            return false;
        }
        profile.addInternalBalance(amount);
        save(profile);
        return true;
    }

    public boolean withdraw(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return false;
        }
        if (vaultPresent()) {
            try {
                Object response = withdrawMethod.invoke(economy, player, Double.valueOf(amount));
                return response == null || success(response);
            } catch (Throwable throwable) {
                switchToInternal("withdrawPlayer", throwable);
            }
        }
        PlayerProfile profile = profileOf(player);
        if (profile == null) {
            return false;
        }
        boolean ok = profile.takeInternalBalance(amount);
        if (ok) {
            save(profile);
        }
        return ok;
    }

    public String format(double amount) {
        if (vaultPresent() && formatMethod != null) {
            try {
                Object result = formatMethod.invoke(economy, Double.valueOf(amount));
                if (result != null) {
                    return String.valueOf(result);
                }
            } catch (Throwable ignored) {
                this.formatMethod = null;
            }
        }
        DecimalFormat formatter;
        try {
            formatter = new DecimalFormat(currencyFormat, DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException exception) {
            formatter = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        }
        return formatter.format(amount);
    }

    private PlayerProfile profileOf(Player player) {
        if (player == null || profiles == null) {
            return null;
        }
        return profiles.get(player.getUniqueId());
    }

    private void save(PlayerProfile profile) {
        if (profiles != null && profile != null) {
            profiles.save(profile);
        }
    }

    private static boolean success(Object response) {
        try {
            Method method = response.getClass().getMethod("transactionSuccess");
            Object result = method.invoke(response);
            if (result instanceof Boolean) {
                return ((Boolean) result).booleanValue();
            }
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void switchToInternal(String action, Throwable throwable) {
        if (plugin != null) {
            plugin.getLogger().warning("[AurumBuyer] Vault " + action + " failed ("
                    + throwable.getClass().getSimpleName() + ") - switching to INTERNAL economy.");
        }
        this.mode = Mode.INTERNAL;
        this.economy = null;
    }
}