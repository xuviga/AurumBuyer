package com.xuvigan.aurumbuyer.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.AurumBuyer;
import com.xuvigan.aurumbuyer.booster.BoosterCategory;
import com.xuvigan.aurumbuyer.placeholder.TopService;

/**
 * Tab completion for every command and subcommand, filtered by permission.
 */
public final class AurumTabCompleter implements TabCompleter {

    private final AurumBuyer plugin;

    public AurumTabCompleter(AurumBuyer plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (!sender.hasPermission("aurum.use")) {
            return out;
        }
        int position = 0;
        boolean admin = false;
        if (args.length == 0) {
            admin = false;
        } else if (args.length == 1) {
            for (String sub : AurumCommand.SUBCOMMANDS) {
                if (allowed(sender, sub)) {
                    out.add(sub);
                }
            }
            return filter(out, args[0]);
        } else if ("admin".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("aurum.admin")) {
                return out;
            }
            admin = true;
            position = 1;
        }

        if (admin) {
            String action = args.length >= 2 ? args[1].toLowerCase(Locale.ENGLISH) : null;
            if (args.length == 2) {
                return filter(AurumCommand.ADMIN_SUBCOMMANDS, args[1]);
            }
            if ("points".equals(action) || "ulta".equals(action)) {
                if (args.length == 3) {
                    return filter(list("give", "take"), args[2]);
                }
                if (args.length == 4) {
                    return filter(players(), args[3]);
                }
                if (args.length == 5) {
                    return filter(list("1", "10", "100", "1000"), args[4]);
                }
            } else if ("level".equals(action)) {
                if (args.length == 3) {
                    return filter(list("set"), args[2]);
                }
                if (args.length == 4) {
                    return filter(players(), args[3]);
                }
                if (args.length == 5) {
                    List<String> categories = new ArrayList<String>();
                    for (BoosterCategory category : plugin.boosters().categories()) {
                        categories.add(category.id());
                    }
                    return filter(categories, args[4]);
                }
                if (args.length == 6) {
                    return filter(list("0", "1", "5", "10", "20"), args[5]);
                }
            } else if ("limit".equals(action)) {
                if (args.length == 3) {
                    return filter(list("reset"), args[2]);
                }
                if (args.length == 4) {
                    List<String> targets = players();
                    targets.add("global");
                    return filter(targets, args[3]);
                }
            } else if ("reset".equals(action)) {
                if (args.length == 3) {
                    return filter(players(), args[2]);
                }
            }
            return out;
        }

        String sub = args[position].toLowerCase(Locale.ENGLISH);
        if ("top".equals(sub)) {
            if (args.length == position + 2) {
                return filter(list("money", "points"), args[position + 1]);
            }
        } else if ("auto".equals(sub)) {
            if (args.length == position + 2) {
                return filter(list("on", "untilquit", "off"), args[position + 1]);
            }
        } else if ("shop".equals(sub) || "history".equals(sub)) {
            if (args.length == position + 2) {
                return filter(list("1", "2", "3"), args[position + 1]);
            }
        }
        return out;
    }

    private boolean allowed(CommandSender sender, String sub) {
        if ("admin".equals(sub) || "reload".equals(sub)) {
            return sender.hasPermission("aurum.admin");
        }
        if ("shop".equals(sub)) {
            return sender.hasPermission("aurum.shop");
        }
        if ("boosters".equals(sub)) {
            return sender.hasPermission("aurum.boosters");
        }
        if ("top".equals(sub)) {
            return sender.hasPermission("aurum.top");
        }
        if ("sell".equals(sub)) {
            return sender.hasPermission("aurum.sell");
        }
        return true;
    }

    private List<String> players() {
        List<String> out = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            out.add(player.getName());
        }
        for (TopService.Entry entry : plugin.tops().money()) {
            if (!out.contains(entry.name())) {
                out.add(entry.name());
            }
        }
        return out;
    }

    private static List<String> list(String... values) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            out.add(values[i]);
        }
        return out;
    }

    private static List<String> filter(List<String> source, String prefix) {
        List<String> out = new ArrayList<String>();
        String key = prefix == null ? "" : prefix.toLowerCase(Locale.ENGLISH);
        for (String value : source) {
            if (value != null && value.toLowerCase(Locale.ENGLISH).startsWith(key)) {
                out.add(value);
            }
        }
        return out;
    }
}
