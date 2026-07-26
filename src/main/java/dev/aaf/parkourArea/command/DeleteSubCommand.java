package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.zone.Zone;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /parkour delete &lt;zoneid/zonename&gt; — 删除区域及后代，需二次确认。 */
public final class DeleteSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public DeleteSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public String permission() {
        return Permission.ADMIN;
    }

    @Override
    public String description() {
        return "删除区域（二次确认）";
    }

    @Override
    public String usage() {
        return "<zoneid/zonename>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return;
        }
        if (!plugin.editModeService().isEditMode(player)) {
            plugin.messages().send(sender, "command.edit-mode-required");
            return;
        }
        // 若有待确认项，直接确认（触发上次注册的删除动作）
        if (plugin.confirmFlow().hasPending(player.getUniqueId())) {
            plugin.confirmFlow().confirm(player.getUniqueId());
            return;
        }
        if (args.length < 1) {
            plugin.messages().send(sender, "command.not-found", Map.of("query", ""));
            return;
        }
        Zone z = plugin.zoneRepository().tree().resolve(args[0]);
        if (z == null) {
            plugin.messages().send(sender, "command.not-found", Map.of("query", args[0]));
            return;
        }
        int descCount = plugin.zoneRepository().tree().descendentsOf(z.id()).size();

        plugin.messages().send(sender, "command.confirm-required",
                Map.of("command", "parkour delete " + args[0]));
        plugin.confirmFlow().request(player.getUniqueId(), confirmed -> {
            if (!confirmed) {
                plugin.messages().send(sender, "command.cancelled");
                return;
            }
            int deleted = plugin.zoneRepository().delete(z.id());
            String childrenSuffix = descCount > 1
                    ? plugin.messages().raw("command.delete-children-suffix",
                            Map.of("n", String.valueOf(deleted - 1)))
                    : "";
            plugin.messages().send(sender, "command.deleted", Map.of(
                    "id", String.valueOf(z.id()),
                    "children", childrenSuffix));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            return plugin.zoneRepository().tree().all().stream()
                    .map(z -> z.name().isEmpty() ? String.valueOf(z.id()) : z.name())
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(p))
                    .toList();
        }
        return List.of();
    }
}
