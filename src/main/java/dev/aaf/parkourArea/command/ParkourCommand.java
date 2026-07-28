package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.config.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * /parkour（别名 pko/pk/po）命令根。分发到子命令。
 * 自写轻量框架，避免引入不稳定 beta 命令库。
 */
public final class ParkourCommand implements CommandExecutor, TabCompleter {

    private final ParkourArea plugin;
    // LinkedHashMap：帮助页按注册顺序稳定显示
    private final Map<String, SubCommand> subs = new LinkedHashMap<>();

    public ParkourCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    public void register(SubCommand sub) {
        subs.put(sub.name().toLowerCase(), sub);
        for (String a : sub.aliases()) {
            subs.put(a.toLowerCase(), sub);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }
        SubCommand sub = subs.get(args[0].toLowerCase());
        if (sub == null) {
            plugin.messages().send(sender, "command.no-permission"); // fallback 简化
            sendHelp(sender, label);
            return true;
        }
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
            plugin.messages().send(sender, "command.no-permission");
            return true;
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        try {
            sub.execute(sender, subArgs);
        } catch (Throwable t) {
            plugin.messages().send(sender, "command.no-permission");
            plugin.getLogger().warning("执行子命令 " + sub.name() + " 时出错: " + t.getMessage());
            t.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            Set<SubCommand> seen = new HashSet<>();
            for (SubCommand sub : subs.values()) {
                if (!seen.add(sub)) {
                    continue;
                }
                if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
                    continue;
                }
                if (sub.name().toLowerCase().startsWith(prefix)) {
                    out.add(sub.name());
                }
            }
            Collections.sort(out);
            return out;
        }
        SubCommand sub = subs.get(args[0].toLowerCase());
        if (sub == null) {
            return Collections.emptyList();
        }
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
            return Collections.emptyList();
        }
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendHelp(CommandSender sender, String label) {
        Messages m = plugin.messages();
        sender.sendMessage(m.plain("help.header"));
        Set<SubCommand> seen = new HashSet<>();
        for (SubCommand sub : subs.values()) {
            if (!seen.add(sub)) {
                continue;
            }
            if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
                continue;
            }
            String usage = sub.usage().isEmpty() ? "" : " " + sub.usage();
            sender.sendMessage(m.plain("help.line",
                    Map.of("command", label + " " + sub.name() + usage, "desc", sub.description())));
        }
        sender.sendMessage(m.plain("help.footer"));
    }
}
