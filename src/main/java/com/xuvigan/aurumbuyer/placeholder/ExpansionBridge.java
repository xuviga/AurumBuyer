package com.xuvigan.aurumbuyer.placeholder;

/**
 * Reflective PlaceholderAPI bridge. The expansion class implements a
 * PlaceholderAPI interface, so it is only loaded when the plugin is present;
 * everything here degrades to a no-op otherwise.
 */
public final class ExpansionBridge {

    private final Object expansion;
    private final java.lang.reflect.Method unregister;

    private ExpansionBridge(Object expansion, java.lang.reflect.Method unregister) {
        this.expansion = expansion;
        this.unregister = unregister;
    }

    public static ExpansionBridge register(final AurumBuyerProvider provider, org.bukkit.plugin.Plugin plugin, String version) {
        try {
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
        } catch (Throwable missing) {
            return null;
        }
        try {
            Object expansion = new AurumExpansion(provider);
            java.lang.reflect.Method canRegister = find(expansion.getClass(), "canRegister");
            if (canRegister != null) {
                Object result = canRegister.invoke(expansion);
                if (result instanceof Boolean && !((Boolean) result).booleanValue()) {
                    return null;
                }
            }
            boolean registered;
            try {
                java.lang.reflect.Method register = find(expansion.getClass(), "register");
                Object result = register.invoke(expansion);
                registered = !(result instanceof Boolean) || ((Boolean) result).booleanValue();
            } catch (Throwable direct) {
                // very old API: fall back to the static registry
                Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method add = api.getMethod("registerExpansion",
                        Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion"));
                Object result = add.invoke(null, expansion);
                registered = !(result instanceof Boolean) || ((Boolean) result).booleanValue();
            }
            if (!registered) {
                return null;
            }
            if (plugin != null) {
                plugin.getLogger().info("[AurumBuyer] PlaceholderAPI expansion 'aurum' registered (v" + version + ").");
            }
            return new ExpansionBridge(expansion, find(expansion.getClass(), "unregister"));
        } catch (Throwable throwable) {
            if (plugin != null) {
                plugin.getLogger().warning("[AurumBuyer] PlaceholderAPI expansion not registered: "
                        + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            }
            return null;
        }
    }

    private static java.lang.reflect.Method find(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    public boolean present() {
        return expansion != null;
    }

    public void unregister() {
        if (expansion == null || unregister == null) {
            return;
        }
        try {
            unregister.invoke(expansion);
        } catch (Throwable ignored) {
            // shutting down
        }
    }
}
