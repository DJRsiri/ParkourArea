package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.zone.Zone;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** /parkour list — 列出所有区域的 id、名称、类型、形状、父级。 */
public final class ListSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public ListSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return "列出所有区域";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Collection<Zone> all = plugin.zoneRepository().tree().all();
        if (all.isEmpty()) {
            plugin.messages().send(sender, "list.empty");
            return;
        }
        plugin.messages().send(sender, "list.header");
        for (Zone z : all) {
            String nameDisplay = z.name().isEmpty() ? "(未命名)" : z.name();
            String parentDisplay = z.parentId() == null ? "-" : String.valueOf(z.parentId());
            sender.sendMessage(plugin.messages().plain("info.line", Map.of(
                    "id", String.valueOf(z.id()),
                    "name", nameDisplay,
                    "type", z.type().name(),
                    "shape", z.shape().name(),
                    "parent", parentDisplay)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
