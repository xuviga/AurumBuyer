package com.xuvigan.aurumbuyer;

import java.io.File;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.xuvigan.aurumbuyer.booster.BoosterService;
import com.xuvigan.aurumbuyer.command.AurumCommand;
import com.xuvigan.aurumbuyer.command.AurumTabCompleter;
import com.xuvigan.aurumbuyer.config.BoosterConfig;
import com.xuvigan.aurumbuyer.config.ConfigManager;
import com.xuvigan.aurumbuyer.config.Messages;
import com.xuvigan.aurumbuyer.config.PricesConfig;
import com.xuvigan.aurumbuyer.config.ShopConfig;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.data.ProfileManager;
import com.xuvigan.aurumbuyer.data.impl.YamlProfileStorage;
import com.xuvigan.aurumbuyer.economy.EconomyBridge;
import com.xuvigan.aurumbuyer.engine.AutoSellTask;
import com.xuvigan.aurumbuyer.engine.LimitService;
import com.xuvigan.aurumbuyer.engine.MultiplierService;
import com.xuvigan.aurumbuyer.engine.SellEngine;
import com.xuvigan.aurumbuyer.gui.GuiManager;
import com.xuvigan.aurumbuyer.gui.InventoryListener;
import com.xuvigan.aurumbuyer.listener.PlayerSessionListener;
import com.xuvigan.aurumbuyer.placeholder.AurumBuyerProvider;
import com.xuvigan.aurumbuyer.placeholder.ExpansionBridge;
import com.xuvigan.aurumbuyer.placeholder.PlaceholderService;
import com.xuvigan.aurumbuyer.placeholder.TopService;

/** AurumBuyer entry point (SPEC.md section 4). */
public final class AurumBuyer extends JavaPlugin implements AurumBuyerProvider {

    private static AurumBuyer instance;

    private ConfigManager config;
    private Messages messages;
    private PricesConfig prices;
    private ShopConfig shop;
    private BoosterConfig boosterConfig;

    private ProfileManager profiles;
    private EconomyBridge economy;
    private SellEngine sell;
    private LimitService limits;
    private MultiplierService multipliers;
    private BoosterService boosters;

    private GuiManager gui;
    private PlaceholderService placeholders;
    private TopService tops;

    private ExpansionBridge expansion;
    private AutoSellTask autoSellTask;
    private org.bukkit.scheduler.BukkitTask autosaveTask;
    private YamlConfiguration messagesYaml;

    public static AurumBuyer get() {
        return instance;
    }

    // ------------------------------------------------------------------- access

    public ConfigManager config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public PricesConfig prices() {
        return prices;
    }

    public ShopConfig shop() {
        return shop;
    }

    public BoosterConfig boosterConfig() {
        return boosterConfig;
    }

    public ProfileManager profiles() {
        return profiles;
    }

    public SellEngine sell() {
        return sell;
    }

    public LimitService limits() {
        return limits;
    }

    public MultiplierService multipliers() {
        return multipliers;
    }

    public BoosterService boosters() {
        return boosters;
    }

    public EconomyBridge economy() {
        return economy;
    }

    public TopService tops() {
        return tops;
    }

    public GuiManager gui() {
        return gui;
    }

    public PlaceholderService placeholders() {
        return placeholders;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void onEnable() {
        instance = this;
        long started = System.currentTimeMillis();
        getLogger().info("Starting AurumBuyer " + getDescription().getVersion() + " by XuViGaN");

        this.config = new ConfigManager(this);
        loadMessages();
        this.prices = new PricesConfig(loadConfiguration("prices.yml"));
        this.shop = new ShopConfig(loadConfiguration("shop.yml"), config);
        this.boosterConfig = new BoosterConfig(loadConfiguration("boosters.yml"));

        YamlProfileStorage storage = new YamlProfileStorage(this,
                new File(getDataFolder(), "playerdata"),
                config.historySize(),
                config.internalStartBalance());
        storage.setPersonalResetHours(config.personalResetHours());
        this.profiles = storage;

        this.economy = new EconomyBridge(this, profiles, config.economyMode().name(), config.currencyFormat());
        this.limits = new LimitService(this, config, new File(getDataFolder(), "globallimits.yml"));
        this.multipliers = new MultiplierService(config);
        this.sell = new SellEngine(this);
        this.boosters = new BoosterService(this);
        this.gui = new GuiManager(this);
        this.placeholders = new PlaceholderService(this);
        this.tops = new TopService(this);
        this.messages.resolver(new Messages.PlaceholderResolver() {
            @Override
            public String resolve(Player player, String text) {
                return placeholders.apply(player, text);
            }
        });

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        registerCommands();
        startAutoSell();
        tops.start();

        for (Player online : getServer().getOnlinePlayers()) {
            PlayerProfile profile = profiles.get(online.getUniqueId());
            profile.name(online.getName());
            if (profile.autoMode() == AutoSellMode.UNTIL_QUIT) {
                profile.autoMode(AutoSellMode.DISABLED);
            }
            if (profile.dirty()) {
                profiles.save(profile);
            }
        }

        registerExpansion();

        getLogger().info("AurumBuyer enabled in " + (System.currentTimeMillis() - started) + " ms (economy: "
                + economy.modeName() + ", items: " + prices.size() + ", boosters: " + boosterConfig.size() + ")");
    }

    @Override
    public void onDisable() {
        if (sell != null && boosters != null) {
            for (PlayerProfile profile : profiles.all()) {
                boosters.cleanup(profile);
                if (profile.autoMode() == AutoSellMode.UNTIL_QUIT) {
                    profile.autoMode(AutoSellMode.DISABLED);
                }
                if (profile.dirty()) {
                    profiles.saveBlocking(profile);
                }
            }
        }
        stopTasks();
        if (tops != null) {
            tops.stop();
        }
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        if (limits != null) {
            limits.saveBlocking();
        }
        instance = null;
        getLogger().info("AurumBuyer disabled");
    }

    @Override
    public void onLoad() {
        // nothing: all state is built in onEnable
    }

    private void startAutoSell() {
        stopTasks();
        if (!config.autoSellEnabled()) {
            startAutosave();
            return;
        }
        autoSellTask = new AutoSellTask(this);
        long interval = config.autoSellIntervalTicks();
        autoSellTask.runTaskTimer(this, interval, interval);
        startAutosave();
    }

    /** Flushes dirty profiles on a schedule (also when auto-sell is disabled). */
    private void startAutosave() {
        long ticks = 20L * Math.max(20, config.saveIntervalSeconds());
        autosaveTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                saveDirtyProfiles();
            }
        }, ticks, ticks);
    }

    private void saveDirtyProfiles() {
        if (profiles == null) {
            return;
        }
        for (PlayerProfile profile : profiles.all()) {
            if (profile.dirty()) {
                profiles.save(profile);
            }
        }
        if (limits != null) {
            limits.flush();
        }
    }

    private void stopTasks() {
        if (autoSellTask != null) {
            try {
                autoSellTask.cancel();
            } catch (Throwable ignored) {
                // already cancelled
            }
            this.autoSellTask = null;
        }
        if (autosaveTask != null) {
            try {
                autosaveTask.cancel();
            } catch (Throwable ignored) {
                // already cancelled
            }
            this.autosaveTask = null;
        }    }

    private void registerCommands() {
        PluginCommand command = getCommand("aurum");
        String name = command == null ? null : command.getName();
        AurumCommand executor = new AurumCommand(this);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(new AurumTabCompleter(this));
            command.setDescription(getDescription().getDescription());
        } else if (name != null) {
            getLogger().warning("[AurumBuyer] Command '" + name + "' is missing from plugin.yml");
        }
        if (name == null) {
            getLogger().warning("[AurumBuyer] Command 'aurum' is not defined in plugin.yml - /aurum is unavailable");
        }
    }

    private void registerExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            this.expansion = ExpansionBridge.register(this, this, getDescription().getVersion());
        } catch (Throwable throwable) {
            getLogger().warning("[AurumBuyer] PlaceholderAPI expansion skipped: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    // ------------------------------------------------------------------- reload

    /** Reloads every configuration file; profiles stay in memory. */
    public void reloadAll() {
        config.reload();
        loadMessages();
        this.prices = new PricesConfig(loadConfiguration("prices.yml"));
        this.shop = new ShopConfig(loadConfiguration("shop.yml"), config);
        this.boosterConfig = new BoosterConfig(loadConfiguration("boosters.yml"));
        this.limits.reload();
        this.multipliers.reload();
        this.economy.configure(config.economyMode().name(), config.currencyFormat());
        this.economy.setup();
        if (profiles instanceof YamlProfileStorage) {
            ((YamlProfileStorage) profiles).setPersonalResetHours(config.personalResetHours());
        }
        startAutoSell();
        if (tops != null) {
            tops.start();
        }
    }

    private void loadMessages() {
        File folder = getDataFolder();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            getLogger().warning("[AurumBuyer] Could not create " + folder.getAbsolutePath());
        }
        File file = new File(folder, "messages.yml");
        if (!file.isFile()) {
            saveResource("messages.yml", false);
        }
        this.messagesYaml = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        // bundled defaults act as a fallback for keys missing from the user file
        try {
            java.io.InputStream stream = getResource("messages.yml");
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
                this.messagesYaml.setDefaults(defaults);
                stream.close();
            }
        } catch (Throwable throwable) {
            getLogger().warning("[AurumBuyer] Could not read bundled messages.yml: " + throwable.getMessage());
        }
        String pointsName = config.currencyName();
        if (this.messages == null) {
            this.messages = new Messages(this.messagesYaml, pointsName);
        } else {
            this.messages.reload(this.messagesYaml, pointsName);
        }
    }

    private YamlConfiguration loadConfiguration(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.isFile()) {
            saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // ---------------------------------------------------------- AurumBuyerProvider

    @Override
    public String version() {
        return getDescription() == null ? "1.0.0" : getDescription().getVersion();
    }

    @Override
    public String resolve(Player player, String identifier) {
        return placeholders == null ? "" : placeholders.resolve(player, identifier);
    }
}
