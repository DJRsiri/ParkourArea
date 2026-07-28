package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.hooks.worldedit.SelectionInfo;
import dev.aaf.parkourArea.zone.SelectionShape;
import dev.aaf.parkourArea.zone.ValidationResult;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneSpawn;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
 *   <li>{@code spawn [here]}（取当前位置与朝向为传送点，留空默认 here）</li>
 *   <li>{@code spawn <x> <y> <z> <yaw> <pitch>}（全量设置传送点）</li>
 *   <li>{@code spawn yaw <yaw> [pitch <pitch>]}（仅改朝向，常用于关卡 LEVEL）</li>
 *   <li>{@code spawn clear}（清除传送点）</li>
 *   <li>{@code resize <x1> <y1> <z1> <x2> <y2> <z2>}（CUBOID；可省略坐标用 WorldEdit 选区）/ {@code resize <cx> <cy> <cz> <radius>}（SPHERE）</li>
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
        return "编辑区域（rename / spawn / resize ...）";
    }

    @Override
    public String usage() {
        return "<zoneid/zonename> rename <newname> | spawn [here] | spawn <x> <y> <z> <yaw> <pitch> | spawn yaw <yaw> [pitch <pitch>] | spawn clear | resize <x1> <y1> <z1> <x2> <y2> <z2> | resize <cx> <cy> <cz> <radius> | resize (WE 选区)";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
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
                if (args.length < 3) {
                    usage(sender);
                    return;
                }
                String newName = args[2];
                plugin.zoneRepository().rename(z.id(), newName);
                plugin.messages().send(sender, "command.created", Map.of(
                        "id", String.valueOf(z.id()),
                        "name", newName,
                        "type", z.type().name(),
                        "shape", z.shape().name()));
            }
            case "spawn" -> handleSpawn(sender, z, Arrays.copyOfRange(args, 2, args.length));
            case "resize" -> handleResize(sender, z, Arrays.copyOfRange(args, 2, args.length));
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
        // here（或留空默认）：取玩家当前位置与朝向作为传送点
        if (a.length == 0 || "here".equalsIgnoreCase(a[0])) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "command.player-only");
                return;
            }
            var loc = player.getLocation();
            plugin.zoneRepository().setSpawn(z.id(), ZoneSpawn.of(
                    loc.getX(), loc.getY(), loc.getZ(),
                    (double) loc.getYaw(), (double) loc.getPitch()));
            plugin.messages().send(sender, "command.spawn-set",
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

    private void handleResize(CommandSender sender, Zone z, String[] a) {
        Zone newGeo;
        if (z.shape() == SelectionShape.CUBOID) {
            int[] c;
            if (a.length == 6) {
                c = new int[6];
                for (int i = 0; i < 6; i++) {
                    try {
                        c[i] = Integer.parseInt(a[i]);
                    } catch (NumberFormatException e) {
                        plugin.messages().send(sender, "command.invalid-hierarchy", Map.of(
                                "reason", "坐标参数错误，cuboid 需要 6 个整数: <x1> <y1> <z1> <x2> <y2> <z2>"));
                        return;
                    }
                }
            } else if (a.length == 0) {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "command.player-only");
                    return;
                }
                SelectionInfo sel = plugin.worldEditHook().getCuboidSelection(player);
                if (sel == null) {
                    plugin.messages().send(sender, "command.need-selection");
                    return;
                }
                if (!java.util.Objects.equals(sel.worldUid(), z.worldUid())) {
                    plugin.messages().send(sender, "command.invalid-hierarchy",
                            Map.of("reason", "选区世界与区域世界不一致"));
                    return;
                }
                c = new int[]{sel.x1(), sel.y1(), sel.z1(), sel.x2(), sel.y2(), sel.z2()};
            } else {
                plugin.messages().send(sender, "command.invalid-hierarchy", Map.of(
                        "reason", "坐标参数数量错误，cuboid 需要 6 个整数（或留空用 WorldEdit 选区）"));
                return;
            }
            newGeo = Zone.cuboid(z.id(), z.name(), z.type(), z.worldUid(), z.parentId(),
                    c[0], c[1], c[2], c[3], c[4], c[5]);
        } else {
            if (a.length != 4) {
                usage(sender);
                return;
            }
            Double cx = parseDouble(a[0]), cy = parseDouble(a[1]), cz = parseDouble(a[2]),
                    r = parseDouble(a[3]);
            if (cx == null || cy == null || cz == null || r == null) {
                usage(sender);
                return;
            }
            if (r <= 0) {
                plugin.messages().send(sender, "command.invalid-hierarchy",
                        Map.of("reason", "半径必须为正数"));
                return;
            }
            newGeo = Zone.sphere(z.id(), z.name(), z.type(), z.worldUid(), z.parentId(), cx, cy, cz, r);
        }
        ValidationResult vr = plugin.zoneRepository().resize(z.id(), newGeo);
        if (!vr.valid()) {
            plugin.messages().send(sender, "command.invalid-hierarchy", Map.of("reason", vr.reason()));
            return;
        }
        plugin.messages().send(sender, "command.resized", Map.of("id", String.valueOf(z.id())));
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
            return java.util.stream.Stream.of("rename", "spawn", "resize").filter(s -> s.startsWith(p)).toList();
        }
        if (args.length == 3 && "spawn".equalsIgnoreCase(args[1])) {
            String p = args[2].toLowerCase(Locale.ROOT);
            return java.util.stream.Stream.of("clear", "here", "yaw", "pitch").filter(s -> s.startsWith(p)).toList();
        }
        return List.of();
    }
}
