package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.player.ParkourPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /parkour sound &lt;checkpoint|block&gt; &lt;on|off&gt; — 切换音效子项（受总开关闭锁）。 */
public final class SoundSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public SoundSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "sound";
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return "切换音效子项";
    }

    @Override
    public String usage() {
        return "<checkpoint|block> <on|off>";
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
        if (args.length < 2) {
            plugin.messages().send(sender, "command.sound-usage");
            return;
        }
        String which = args[0].toLowerCase(Locale.ROOT);
        Boolean on = parseBool(args[1]);
        if (on == null) {
            plugin.messages().send(sender, "command.sound-usage");
            return;
        }
        boolean val = on;
        switch (which) {
            case "checkpoint", "cp" -> {
                session.checkpointSoundEnabled(val);
                plugin.messages().send(sender, val ? "command.sound-checkpoint-on" : "command.sound-checkpoint-off");
            }
            case "block", "bk" -> {
                session.blockSoundEnabled(val);
                plugin.messages().send(sender, val ? "command.sound-block-on" : "command.sound-block-off");
            }
            default -> plugin.messages().send(sender, "command.sound-usage");
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
            return filter(List.of("checkpoint", "block"), args[0]);
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
