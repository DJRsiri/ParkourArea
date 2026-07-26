package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneSpawn;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /parkour edit &lt;zoneid/zonename&gt; &lt;action&gt; ...
 *
 * <p>支持：
 * <ul>
 *   <li>{@code rename <newname>}</li>
 *   <li>{@code spawn <x> <y> <z> <yaw> <pitch>}（全量设置传送点）</li>
 *   <li>{@code spawn yaw <yaw> [pitch <pitch>]}（仅改朝向，常用于关卡 LEVEL）</li>
 *   <li>{@code spawn clear}（清除传送点）</li>
 * </ul>
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
        return "编辑区域（rename / spawn ...）";
    }

    @Override
    public String usage() {
        return "<zoneid/zonename> rename <newname> | spawn <x> <y> <z> <yaw> <pitch> | spawn yaw <yaw> [pitch <pitch>] | spawn clear";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender);
            return;
        }
        Zone z = plugin.zoneRepository().tree().resolve(args[0]);
        if (z == null) {
            plugin.messages().send(sender, "command.not-found", Map.of("query", args[0]));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "rename" -> {
                String newName = args[2];
                plugin.zoneRepository().rename(z.id(), newName);
                plugin.messages().send(sender, "command.created", Map.of(
                        "id", String.valueOf(z.id()),
                        "name", newName,
                        "type", z.type().name(),
                        "shape", z.shape().name()));
            }
            case "spawn" -> handleSpawn(sender, z, Arrays.copyOfRange(args, 2, args.length));
            default -> usage(sender);
        }
    }

    private void handleSpawn(CommandSender sender, Zone z, String[] a) {
        if (a.length >= 1 && "clear".equalsIgnoreCase(a[0])) {
            plugin.zoneRepository().setSpawn(z.id(), null);
            plugin.messages().send(sender, "command.spawn-cleared",
                    Map.of("id", String.valueOf(z.id())));
            return;
        }
        // 部分更新：yaw / pitch 关键字（用于"关卡朝向"主用例）
        if (a.length >= 1 && ("yaw".equalsIgnoreCase(a[0]) || "pitch".equalsIgnoreCase(a[0]))) {
            Double yaw = null, pitch = null;
            int i = 0;
            while (i < a.length) {
                if ("yaw".equalsIgnoreCase(a[i]) && i + 1 < a.length) {
                    yaw = parseDouble(a[i + 1]);
                    if (yaw == null) { usage(sender); return; }
                    i += 2;
                } else if ("pitch".equalsIgnoreCase(a[i]) && i + 1 < a.length) {
                    pitch = parseDouble(a[i + 1]);
                    if (pitch == null) { usage(sender); return; }
                    i += 2;
                } else {
                    usage(sender);
                    return;
                }
            }
            ZoneSpawn base = z.spawn() == null ? ZoneSpawn.of(null, null, null, null, null) : z.spawn();
            plugin.zoneRepository().setSpawn(z.id(), base.merge(null, null, null, yaw, pitch));
            plugin.messages().send(sender, "command.spawn-set",
                    Map.of("id", String.valueOf(z.id())));
            return;
        }
        // 全量：x y z yaw pitch
        if (a.length >= 5) {
            Double x = parseDouble(a[0]), y = parseDouble(a[1]), zz = parseDouble(a[2]),
                    yaw = parseDouble(a[3]), pitch = parseDouble(a[4]);
            if (x == null || y == null || zz == null || yaw == null || pitch == null) {
                usage(sender);
                return;
            }
            plugin.zoneRepository().setSpawn(z.id(), ZoneSpawn.of(x, y, zz, yaw, pitch));
            plugin.messages().send(sender, "command.spawn-set",
                    Map.of("id", String.valueOf(z.id())));
            return;
        }
        usage(sender);
    }

    private void usage(CommandSender sender) {
        plugin.messages().send(sender, "command.invalid-hierarchy",
                Map.of("reason", "用法: edit " + usage()));
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
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
            return java.util.stream.Stream.of("rename", "spawn").filter(s -> s.startsWith(p)).toList();
        }
        if (args.length == 3 && "spawn".equalsIgnoreCase(args[1])) {
            String p = args[2].toLowerCase(Locale.ROOT);
            return java.util.stream.Stream.of("clear", "yaw", "pitch").filter(s -> s.startsWith(p)).toList();
        }
        return List.of();
    }
}
