package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.zone.Zone;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /parkour edit &lt;zoneid/zonename&gt; &lt;action&gt; ...
 *
 * <p>阶段 B 支持：{@code rename <newname>}。坐标/层级编辑在阶段 H 增强。</p>
 */
public final class EditSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public EditSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public String permission() {
        return Permission.ADMIN;
    }

    @Override
    public String description() {
        return "编辑区域（rename ...）";
    }

    @Override
    public String usage() {
        return "<zoneid/zonename> rename <newname>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "command.invalid-hierarchy",
                    Map.of("reason", "用法: edit <zone> rename <newname>"));
            return;
        }
        Zone z = plugin.zoneRepository().tree().resolve(args[0]);
        if (z == null) {
            plugin.messages().send(sender, "command.not-found", Map.of("query", args[0]));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if ("rename".equals(action)) {
            String newName = args[2];
            plugin.zoneRepository().rename(z.id(), newName);
            plugin.messages().send(sender, "command.created", Map.of(
                    "id", String.valueOf(z.id()),
                    "name", newName,
                    "type", z.type().name(),
                    "shape", z.shape().name()));
        } else {
            plugin.messages().send(sender, "command.invalid-hierarchy",
                    Map.of("reason", "未知操作: " + action + "（当前支持 rename）"));
        }
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
        if (args.length == 2) {
            String p = args[1].toLowerCase(Locale.ROOT);
            return java.util.stream.Stream.of("rename").filter(s -> s.startsWith(p)).toList();
        }
        return List.of();
    }
}
