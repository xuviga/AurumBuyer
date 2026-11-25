package com.xuvigan.aurumbuyer.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.booster.BoosterCategory;
import com.xuvigan.aurumbuyer.data.AutoSellMode;
import com.xuvigan.aurumbuyer.data.PlayerProfile;
import com.xuvigan.aurumbuyer.engine.SellResult;
import com.xuvigan.aurumbuyer.engine.SellSource;
import com.xuvigan.aurumbuyer.placeholder.TopService;
import com.xuvigan.aurumbuyer.util.Numbers;
import com.xuvigan.aurumbuyer.util.SoundUtil;

/** {@code /aurum} and its {@code admin} subtree (SPEC.md section 10.11). */
public final class AurumCommand implements CommandExecutor {

    public static final List<String> SUBCOMMANDS = new ArrayList<String>();
    public static final List<String> ADMIN_SUBCOMMANDS = new ArrayList<String>();

    static {
        SUBCOMMANDS.add("help");
        SUBCOMMANDS.add("settings");
        SUBCOMMANDS.add("auto");
        SUBCOMMANDS.add("shop");
        SUBCOMMANDS.add("boosters");
        SUBCOMMANDS.add("history");
        SUBCOMMANDS.add("limits");
        SUBCOMMANDS.add("sell");
        SUBCOMMANDS.add("top");
        SUBCOMMANDS.add("reload");
        SUBCOMMANDS.add("admin");

        ADMIN_SUBCOMMANDS.add("reload");
        ADMIN_SUBCOMMANDS.add("save");
        ADMIN_SUBCOMMANDS.add("points");
        ADMIN_SUBCOMMANDS.add("ulta");
        ADMIN_SUBCOMMANDS.add("level");
        ADMIN_SUBCOMMANDS.add("limit");
        ADMIN_SUBCOMMANDS.add("reset");
    }

    private final AurumBuyer plugin;

    public AurumCommand(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                openPlayerMenu(sender);
                return true;
            }
            String sub = args[0].toLowerCase(java.util.Locale.ENGLISH);
            if ("admin".equals(sub)) {
                return admin(sender, args);
            }
            if ("help".equals(sub)) {
                help(sender);
                return true;
            }
            if ("reload".equals(sub)) {
                if (!sender.hasPermission("aurum.admin")) {
                    plugin.messages().send(sender, "no-permission");
                    return true;
                }
                plugin.reloadAll();
                plugin.messages().send(sender, "reload-done");
                return true;
            }
            if (!sender.hasPermission("aurum.use")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            if ("settings".equals(sub)) {
                Player player = player(sender);
                if (player != null) {
                    plugin.gui().openSettings(player);
                }
                return true;
            }
            if ("auto".equals(sub)) {
                auto(sender, args);
                return true;
            }
            if ("shop".equals(sub)) {
                Player player = player(sender);
                if (player != null) {
                    if (plugin.config().shopRequirePermission() && !player.hasPermission("aurum.shop")) {
                        plugin.messages().send(player, "no-permission");
                        return true;
                    }
                    plugin.gui().openShop(player, page(args, 1));
                }
                return true;
            }
            if ("boosters".equals(sub)) {
                Player player = player(sender);
                if (player != null) {
                    if (!player.hasPermission("aurum.boosters")) {
                        plugin.messages().send(player, "no-permission");
                        return true;
                    }
                    plugin.gui().openBoosters(player);
                }
                return true;
            }
            if ("history".equals(sub)) {
                Player player = player(sender);
                if (player != null) {
                    plugin.gui().openHistory(player, page(args, 1));
                }
                return true;
            }
            if ("limits".equals(sub)) {
                Player player = player(sender);
                if (player != null) {
                    plugin.gui().openLimits(player);
                }
                return true;
            }
            if ("sell".equals(sub)) {
                sell(sender);
                return true;
            }
            if ("top".equals(sub)) {
                top(sender, args);
                return true;
            }
            plugin.messages().send(sender, "unknown-command");
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[AurumBuyer] Command '" + label + "' failed: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return true;
        }
    }

    // -------------------------------------------------------------------- player

    private void openPlayerMenu(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("aurum.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        plugin.gui().openMain(player);
    }

    private void help(CommandSender sender) {
        plugin.messages().sendRaw(sender, plugin.messages().get("help-title"));
        for (String line : plugin.messages().list("help-lines")) {
            plugin.messages().sendRaw(sender, line);
        }
        if (sender.hasPermission("aurum.admin")) {
            plugin.messages().sendRaw(sender, "&e/aurum admin &7- " + plugin.messages().get("admin-usage"));
        }
    }

    private void auto(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        if (plugin.config().autoSellRequirePermission() && !player.hasPermission("aurum.auto")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId());
        AutoSellMode mode;
        if (args.length < 2) {
            mode = profile.autoMode().next();
        } else {
            String value = args[1].toLowerCase(java.util.Locale.ENGLISH);
            if ("on".equals(value) || "true".equals(value) || "enable".equals(value)) {
                mode = AutoSellMode.ENABLED;
            } else if ("untilquit".equals(value) || "until_quit".equals(value) || "quit".equals(value)) {
                mode = AutoSellMode.UNTIL_QUIT;
            } else if ("off".equals(value) || "false".equals(value) || "disable".equals(value)) {
                mode = AutoSellMode.DISABLED;
            } else {
                plugin.messages().send(player, "unknown-command");
                return;
            }
        }
        if (mode.active() && !plugin.config().autoSellEnabled()) {
            mode = AutoSellMode.DISABLED;
        }
        profile.autoMode(mode);
        plugin.profiles().save(profile);
        if (mode == AutoSellMode.ENABLED) {
            plugin.messages().send(player, "auto-enabled");
        } else if (mode == AutoSellMode.UNTIL_QUIT) {
            plugin.messages().send(player, "auto-until-quit");
        } else {
            plugin.messages().send(player, "auto-disabled");
        }
        if (plugin.config().soundEnabled()) {
            SoundUtil.play(player, plugin.config().sound(mode.active() ? "success" : "click"));
        }
    }

    private void sell(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("aurum.sell")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        SellResult result = plugin.sell().sellInventory(player, SellSource.MANUAL);
        if (result.empty()) {
            // a fully blocked sale still carries limit information
            sendLimits(player, result);
            if (result.limitHits().isEmpty()) {
                plugin.messages().send(player, "sell-empty");
            }
            if (plugin.config().soundEnabled()) {
                SoundUtil.play(player, plugin.config().sound("error"));
            }
            return;
        }
        plugin.messages().send(player, "sell-all-done",
                "count", String.valueOf(result.itemCount()),
                "money", plugin.economy().format(result.money()),
                "points", Numbers.round(result.points()));
        sendLimits(player, result);
        if (plugin.config().soundEnabled()) {
            SoundUtil.play(player, plugin.config().sound("sell"));
        }
    }

    private void sendLimits(Player player, SellResult result) {
        List<String> hits = result.limitHits();
        for (String hit : hits) {
            int separator = hit.lastIndexOf('|');
            String item = separator > 0 ? hit.substring(0, separator) : hit;
            String left = separator > 0 ? hit.substring(separator + 1) : "0";
            plugin.messages().send(player, "sell-limit-hit", "item", item, "left", left);
        }
    }

    private void top(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aurum.top")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        sendTop(sender, args);
    }

    private void sendTop(CommandSender sender, String[] args) {
        boolean points = args.length > 1 && "points".equalsIgnoreCase(args[1]);
        List<TopService.Entry> entries = points ? plugin.tops().points() : plugin.tops().money();
        String type = plugin.messages().get(points ? "top-type-points" : "top-type-money", points ? "points" : "money");
        plugin.messages().sendRaw(sender, plugin.messages().get("top-header")
                .replace("%type%", type).replace("%size%", String.valueOf(entries.size())));
        if (entries.isEmpty()) {
            plugin.messages().sendRaw(sender, plugin.messages().get("top-empty"));
            return;
        }
        int position = 1;
        for (TopService.Entry entry : entries) {
            String amount = points ? Numbers.round(entry.amount()) : plugin.economy().format(entry.amount());
            plugin.messages().sendRaw(sender, plugin.messages().get("top-line")
                    .replace("%position%", String.valueOf(position))
                    .replace("%name%", entry.name())
                    .replace("%amount%", amount));
            position++;
        }
    }

    // --------------------------------------------------------------------- admin

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aurum.admin")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            if (sender instanceof Player) {
                plugin.gui().openAdmin((Player) sender);
            } else {
                plugin.messages().sendRaw(sender, plugin.messages().get("admin-usage"));
            }
            return true;
        }
        String action = args[1].toLowerCase(java.util.Locale.ENGLISH);
        if ("reload".equals(action)) {
            plugin.reloadAll();
            plugin.messages().send(sender, "reload-done");
            return true;
        }
        if ("save".equals(action)) {
            plugin.profiles().saveAllBlocking();
            plugin.limits().saveBlocking();
            plugin.messages().send(sender, "admin-saved");
            return true;
        }
        if ("points".equals(action) || "ulta".equals(action)) {
            return adminGive(sender, args, "ulta".equals(action));
        }
        if ("level".equals(action)) {
            return adminLevel(sender, args);
        }
        if ("limit".equals(action)) {
            return adminLimit(sender, args);
        }
        if ("reset".equals(action)) {
            return adminReset(sender, args);
        }
        plugin.messages().send(sender, "admin-usage");
        return true;
    }

    private boolean adminGive(CommandSender sender, String[] args, boolean ulta) {
        if (args.length < 5) {
            plugin.messages().send(sender, "admin-usage");
            return true;
        }
        String direction = args[2].toLowerCase(java.util.Locale.ENGLISH);
        PlayerProfile profile = findProfile(args[3]);
        if (profile == null) {
            plugin.messages().send(sender, "admin-player-not-found");
            return true;
        }
        double amount = Numbers.parse(args[4], -1.0D);
        if (amount < 0.0D) {
            plugin.messages().send(sender, "admin-invalid-number");
            return true;
        }
        boolean give = !"take".equals(direction);
        boolean applied;
        synchronized (profile) {
            if (give) {
                if (ulta) {
                    profile.addUltaPoints(amount, plugin.config().ultaMaxPoints());
                } else {
                    profile.addPoints(amount);
                }
                applied = true;
            } else {
                applied = ulta ? profile.takeUltaPoints(amount) : profile.takePoints(amount);
            }
        }
        if (!applied) {
            plugin.messages().send(sender, "admin-not-enough", "player", profile.name());
            return true;
        }
        plugin.profiles().save(profile);
        if (plugin.tops() != null) {
            plugin.tops().refreshAsync();
        }
        plugin.messages().send(sender, give ? "admin-given" : "admin-taken",
                "amount", Numbers.round(amount), "player", profile.name());
        return true;
    }

    private boolean adminLevel(CommandSender sender, String[] args) {
        // admin level set <player> <category> <level>
        if (args.length < 6 || !"set".equalsIgnoreCase(args[2])) {
            plugin.messages().send(sender, "admin-usage");
            return true;
        }
        PlayerProfile profile = findProfile(args[3]);
        if (profile == null) {
            plugin.messages().send(sender, "admin-player-not-found");
            return true;
        }
        String category = args[4].toLowerCase(java.util.Locale.ENGLISH);
        BoosterCategory config = plugin.boosters().category(category);
        if (config == null) {
            plugin.messages().send(sender, "admin-invalid-category");
            return true;
        }
        int level = Numbers.parseInt(args[5], -1);
        if (level < 0) {
            plugin.messages().send(sender, "admin-invalid-number");
            return true;
        }
        profile.setBoosterLevel(category, Math.min(level, config.maxLevel()));
        plugin.profiles().save(profile);
        plugin.messages().send(sender, "admin-level-set",
                "category", config.display(), "player", profile.name(),
                "level", String.valueOf(profile.boosterLevel(category)));
        return true;
    }

    private boolean adminLimit(CommandSender sender, String[] args) {
        // admin limit reset <player>|global
        if (args.length < 4 || !"reset".equalsIgnoreCase(args[2])) {
            plugin.messages().send(sender, "admin-usage");
            return true;
        }
        String target = args[3];
        if ("global".equalsIgnoreCase(target)) {
            plugin.limits().resetGlobalAll();
            plugin.messages().send(sender, "admin-limit-reset", "target", plugin.messages().get("admin-global", "global"));
            return true;
        }
        PlayerProfile profile = findProfile(target);
        if (profile == null) {
            plugin.messages().send(sender, "admin-player-not-found");
            return true;
        }
        plugin.limits().resetPersonal(profile);
        plugin.profiles().save(profile);
        plugin.messages().send(sender, "admin-limit-reset", "target", profile.name());
        return true;
    }

    private boolean adminReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "admin-usage");
            return true;
        }
        PlayerProfile profile = findProfile(args[2]);
        if (profile == null) {
            plugin.messages().send(sender, "admin-player-not-found");
            return true;
        }
        String name = profile.name();
        profile.reset();
        profile.internalBalance(plugin.config().internalStartBalance());
        plugin.profiles().save(profile);
        if (plugin.tops() != null) {
            plugin.tops().refreshAsync();
        }
        plugin.messages().send(sender, "admin-reset", "player", name);
        return true;
    }

    // ------------------------------------------------------------------- helpers

    private PlayerProfile findProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            PlayerProfile profile = plugin.profiles().get(online.getUniqueId());
            profile.name(online.getName());
            return profile;
        }
        // name lookup goes through the profile cache/playerdata files only:
        // Server#getOfflinePlayer(String) is deprecated and may block the main
        // thread while resolving the name remotely
        return plugin.profiles().getByName(name);
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        plugin.messages().send(sender, "player-only");
        return null;
    }

    private int page(String[] args, int fallback) {
        if (args.length < 2) {
            return fallback;
        }
        return Math.max(1, Numbers.parseInt(args[1], fallback));
    }
}
