package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.player.ParkourPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /parkour togglesound [all|checkpoint|block] [on|off] — 跑酷音效开关。
 *
 * <p>选项留空默认 all（总开关，与音效工具联动）；on/off 留空为切换当前状态。
 * 子项（checkpoint/block）受总开关闭锁。</p>
 */
public final class ToggleSoundSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public ToggleSoundSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "togglesound";
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return "切换跑酷音效（all/checkpoint/block，留空默认 all）";
    }

    @Override
    public String usage() {
        return "[all|checkpoint|block] [on|off]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return;
        }
        ParkourPlayer session = plugin.sessionService().get(player.getUniqueId());
        if (session == null) {
            return;
        }
        String which = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "all";
        Boolean target = null;
        if (args.length >= 2) {
            target = parseBool(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "command.togglesound-usage");
                return;
            }
        }
        switch (which) {
            case "all" -> {
                boolean next = target != null ? target : !session.soundEnabled();
                session.soundEnabled(next);
                plugin.messages().send(sender, next ? "command.sound-master-on" : "command.sound-master-off");
            }
            case "checkpoint", "cp" -> {
                boolean next = target != null ? target : !session.checkpointSoundEnabled();
                session.checkpointSoundEnabled(next);
                plugin.messages().send(sender, next ? "command.sound-checkpoint-on" : "command.sound-checkpoint-off");
            }
            case "block", "bk" -> {
                boolean next = target != null ? target : !session.blockSoundEnabled();
                session.blockSoundEnabled(next);
                plugin.messages().send(sender, next ? "command.sound-block-on" : "command.sound-block-off");
            }
            default -> {
                plugin.messages().send(sender, "command.togglesound-usage");
                return;
            }
        }
        if (plugin.preferenceService() != null) {
            plugin.preferenceService().saveAsync(player.getUniqueId(), session);
        }
    }

    private static Boolean parseBool(String s) {
        if (s == null) {
            return null;
        }
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "on", "true", "1", "yes" -> true;
            case "off", "false", "0", "no" -> false;
            default -> null;
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(List.of("all", "checkpoint", "block"), args[0]);
        }
        if (args.length == 2) {
            return filter(List.of("on", "off"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> opts, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return opts.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
