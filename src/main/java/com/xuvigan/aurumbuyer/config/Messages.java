package com.xuvigan.aurumbuyer.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.xuvigan.aurumbuyer.util.Text;

/**
 * messages.yml accessor.
 *
 * <p>Three substitutions are applied at load time:</p>
 * <ol>
 *   <li>the literal token {@code {points}} becomes the configured currency
 *       name ({@code points-name}, default {@code &6Аурумы});</li>
 *   <li>{@code #RRGGBB} hex colours become section codes;</li>
 *   <li>{@code &a} style codes are translated.</li>
 * </ol>
 * {@code %aurum_*%} placeholders are left for PlaceholderService.apply().
 */
public final class Messages {

    /** Optional bridge that expands {@code %aurum_*%} tokens for a player. */
    public interface PlaceholderResolver {
        String resolve(Player player, String text);
    }

    private FileConfiguration cfg;
    private String pointsName;
    private String prefix;
    private PlaceholderResolver resolver;

    public Messages(FileConfiguration cfg, String pointsName) {
        apply(cfg, pointsName);
    }

    public void reload(FileConfiguration cfg, String pointsName) {
        apply(cfg, pointsName);
    }

    private void apply(FileConfiguration config, String fallbackPoints) {
        this.cfg = config;
        String configured = config == null ? null : config.getString("points-name");
        if (configured == null || configured.trim().isEmpty() || "{points}".equals(configured.trim())) {
            configured = fallbackPoints;
        }
        // first pass: colour the raw currency name and use it as the token value
        this.pointsName = Text.color(configured == null ? "" : configured);
        this.prefix = prepare(config == null ? "" : config.getString("prefix", ""));
    }

    public String pointsName() {
        return pointsName;
    }

    /** Installs the placeholder bridge used by {@link #send} and {@link #sendRaw}. */
    public void resolver(PlaceholderResolver resolver) {
        this.resolver = resolver;
    }

    public String prefix() {
        return prefix;
    }

    public String raw(String path, String def) {
        if (cfg == null) {
            return def == null ? "" : def;
        }
        String value = cfg.getString(path);
        return value == null ? (def == null ? "" : def) : value;
    }

    private String prepare(String text) {
        if (text == null) {
            return "";
        }
        return Text.color(text.replace("{points}", pointsName == null ? "" : pointsName));
    }

    public String get(String path) {
        return prepare(raw(path, ""));
    }

    public String get(String path, String def) {
        return prepare(raw(path, def));
    }

    public String format(String path, Object... pairs) {
        return Text.formatPairs(get(path), pairs);
    }

    public String format(String path, Map<String, String> placeholders) {
        return Text.format(get(path), placeholders);
    }

    public String message(String path, Object... pairs) {
        String body = format(path, pairs);
        String pre = prefix();
        if (body.isEmpty()) {
            return pre;
        }
        return pre + body;
    }

    public List<String> list(String path) {
        List<String> rawList = cfg == null ? null : cfg.getStringList(path);
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>(rawList.size());
        for (String line : rawList) {
            out.add(prepare(line));
        }
        return out;
    }

    public void send(CommandSender target, String path, Object... pairs) {
        if (target == null) {
            return;
        }
        String text = resolve(target, message(path, pairs));
        if (!text.isEmpty()) {
            target.sendMessage(text);
        }
    }

    /** Sends one line with colours and {points} applied (no prefix). */
    public void sendRaw(CommandSender target, String text) {
        if (target == null || text == null || text.isEmpty()) {
            return;
        }
        target.sendMessage(resolve(target, prepare(text)));
    }

    private String resolve(CommandSender target, String text) {
        if (text == null || text.isEmpty() || resolver == null || !(target instanceof Player)) {
            return text;
        }
        try {
            return resolver.resolve((Player) target, text);
        } catch (Throwable ignored) {
            return text;
        }
    }
}
