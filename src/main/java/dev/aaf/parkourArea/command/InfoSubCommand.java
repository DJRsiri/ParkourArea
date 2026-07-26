package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.zone.SelectionShape;
import dev.aaf.parkourArea.zone.Zone;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /parkour info 与 /parkour info-all 共用此类。
 * info 仅显示最具体的命中区域；info-all 显示该位置由大到小的整条区域链。
 */
public final class InfoSubCommand implements SubCommand {

    private final ParkourArea plugin;
    private final String cmdName;
    private final boolean showAll;

    public InfoSubCommand(ParkourArea plugin, String cmdName, boolean showAll) {
        this.plugin = plugin;
        this.cmdName = cmdName;
        this.showAll = showAll;
    }

    @Override
    public String name() {
        return cmdName;
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return showAll ? "显示当前位置所有关系区域" : "显示区域信息";
    }

    @Override
    public String usage() {
        return "[zoneid/zonename | pos <x> <y> <z>]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Target target = resolveTarget(sender, args);
        if (target == null) {
            return;
        }

        if (target.zone != null) {
            if (showAll) {
                plugin.messages().send(sender, "info.header");
                Zone cur = target.zone;
                Set<Integer> visited = new HashSet<>();
                while (cur != null && visited.add(cur.id())) {
                    renderZone(sender, cur);
                    cur = cur.parentId() == null ? null : plugin.zoneRepository().tree().getById(cur.parentId());
                }
            } else {
                renderZone(sender, target.zone);
            }
            return;
        }

        List<Zone> chain = plugin.zoneRepository().tree().findChain(target.world, target.x, target.y, target.z);
        if (chain.isEmpty()) {
            plugin.messages().send(sender, "info.none-at-pos");
            return;
        }
        if (showAll) {
            plugin.messages().send(sender, "info.header");
            for (Zone z : chain) {
                renderZone(sender, z);
            }
        } else {
            renderZone(sender, chain.get(0));
        }
    }

    private void renderZone(CommandSender sender, Zone z) {
        String nameDisplay = z.name().isEmpty() ? "(未命名)" : z.name();
        String parentDisplay = z.parentId() == null ? "-" : String.valueOf(z.parentId());
        sender.sendMessage(plugin.messages().plain("info.line", Map.of(
                "id", String.valueOf(z.id()),
                "name", nameDisplay,
                "type", z.type().name(),
                "shape", z.shape().name(),
                "parent", parentDisplay)));
        if (z.shape() == SelectionShape.CUBOID) {
            sender.sendMessage(plugin.messages().plain("info.bounds-cuboid", Map.of(
                    "x1", String.valueOf(z.minX()), "y1", String.valueOf(z.minY()), "z1", String.valueOf(z.minZ()),
                    "x2", String.valueOf(z.maxX()), "y2", String.valueOf(z.maxY()), "z2", String.valueOf(z.maxZ()))));
        } else {
            sender.sendMessage(plugin.messages().plain("info.bounds-sphere", Map.of(
                    "x", String.valueOf(z.centerX()), "y", String.valueOf(z.centerY()),
                    "z", String.valueOf(z.centerZ()), "r", String.valueOf(z.radius()))));
        }
        World w = z.worldUid() == null ? null : Bukkit.getWorld(z.worldUid());
        sender.sendMessage(plugin.messages().plain("info.world", Map.of(
                "world", w == null ? String.valueOf(z.worldUid()) : w.getName())));
    }

    private Target resolveTarget(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                plugin.messages().send(sender, "command.player-only");
                return null;
            }
            var loc = p.getLocation();
            return Target.position(p.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ());
        }
        if ("pos".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player p)) {
                plugin.messages().send(sender, "command.player-only");
                return null;
            }
            if (args.length < 4) {
                plugin.messages().send(sender, "command.invalid-hierarchy",
                        Map.of("reason", "用法: info pos <x> <y> <z>"));
                return null;
            }
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                return Target.position(p.getWorld().getUID(), x, y, z);
            } catch (NumberFormatException e) {
                plugin.messages().send(sender, "command.invalid-hierarchy", Map.of("reason", "坐标格式错误"));
                return null;
            }
        }
        Zone z = plugin.zoneRepository().tree().resolve(args[0]);
        if (z == null) {
            plugin.messages().send(sender, "command.not-found", Map.of("query", args[0]));
            return null;
        }
        return Target.zone(z);
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

    private record Target(Zone zone, UUID world, double x, double y, double z) {
        static Target zone(Zone z) {
            return new Target(z, null, 0, 0, 0);
        }

        static Target position(UUID w, double x, double y, double z) {
            return new Target(null, w, x, y, z);
        }
    }
}
