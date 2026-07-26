package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /parkour resetsave &lt;levelzone&gt; — 删除玩家在某关的存档（进度+计时+最佳+存档点），二次确认。
 *
 * <p>删档后，该关回到 NONE，玩家不能再选超过被删档点的关卡。</p>
 */
public final class ResetSaveSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public ResetSaveSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "resetsave";
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return "删除某关存档（二次确认）";
    }

    @Override
    public String usage() {
        return "<levelzoneid/zonename>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return;
        }
        if (plugin.confirmFlow().hasPending(player.getUniqueId())) {
            plugin.confirmFlow().confirm(player.getUniqueId());
            return;
        }
        if (args.length < 1) {
            plugin.messages().send(sender, "command.not-found", Map.of("query", ""));
            return;
        }
        Zone z = plugin.zoneRepository().tree().resolve(args[0]);
        if (z == null || z.type() != ZoneType.LEVEL) {
            plugin.messages().send(sender, "command.invalid-hierarchy",
                    Map.of("reason", "未找到关卡区域: " + args[0]));
            return;
        }
        final int levelId = z.id();
        plugin.messages().send(sender, "command.confirm-required",
                Map.of("command", "parkour resetsave " + args[0]));
        plugin.confirmFlow().request(player.getUniqueId(), confirmed -> {
            if (!confirmed) {
                plugin.messages().send(sender, "command.cancelled");
                return;
            }
            plugin.progressService().deleteProgressAsync(player.getUniqueId(), levelId);
            ParkourPlayer session = plugin.sessionService().get(player.getUniqueId());
            if (session != null) {
                session.clearCheckpoint();
            }
            plugin.messages().send(sender, "command.deleted", Map.of(
                    "id", String.valueOf(levelId), "children", ""));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            return plugin.zoneRepository().tree().all().stream()
                    .filter(z -> z.type() == ZoneType.LEVEL)
                    .map(z -> z.name().isEmpty() ? String.valueOf(z.id()) : z.name())
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(p))
                    .toList();
        }
        return List.of();
    }
}
