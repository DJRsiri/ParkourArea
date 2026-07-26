package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.hooks.worldedit.SelectionInfo;
import dev.aaf.parkourArea.zone.SelectionShape;
import dev.aaf.parkourArea.zone.ValidationResult;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneSpec;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /parkour create &lt;type&gt; &lt;name|"-"&gt; [parent|"-"|"here"|#id] [shape] &lt;coords...&gt;
 *
 * <p>type 后紧跟 name（"-" 表示空）。parent 可省略，默认按 "here" 依玩家站位自动推断；
 * GLOBAL 用 "-" 或省略。shape 默认 cuboid；sphere 仅 start/end 可用。
 * 坐标缺失时若安装了 WorldEdit 则用其 cuboid 选区。</p>
 */
public final class CreateSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public CreateSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public String permission() {
        return Permission.ADMIN;
    }

    @Override
    public String description() {
        return "创建区域（parent 留空默认 here）";
    }

    @Override
    public String usage() {
        return "<type> <name|-> [parent|-|here] [cuboid|sphere] <coords...>";
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
        if (args.length < 2) {
            plugin.messages().send(sender, "command.invalid-hierarchy",
                    Map.of("reason", "参数不足。用法: " + usage()));
            return;
        }

        ZoneType type = ZoneType.parse(args[0]);
        if (type == null) {
            plugin.messages().send(sender, "command.invalid-hierarchy",
                    Map.of("reason", "未知区域类型: " + args[0]));
            return;
        }

        String name = "-".equals(args[1]) ? "" : args[1];

        // 从 args[2] 起扫描：shape 关键字 / parent（-、here、#id、区域名）各消费一次，
        // 剩下的视为坐标（纯数字开头）。parent 留空默认 here 推断。
        SelectionShape shape = SelectionShape.CUBOID;
        boolean shapeSet = false;
        Integer parentId = null;
        boolean parentHere = false;
        boolean parentSet = false;
        int idx = 2;
        while (idx < args.length) {
            String tok = args[idx];
            if (!shapeSet) {
                SelectionShape s = SelectionShape.parse(tok);
                if (s != null) {
                    shape = s;
                    shapeSet = true;
                    idx++;
                    continue;
                }
            }
            if (!parentSet) {
                if ("-".equals(tok)) {
                    parentSet = true;
                    idx++;
                    continue;
                }
                if ("here".equalsIgnoreCase(tok)) {
                    parentHere = true;
                    parentSet = true;
                    idx++;
                    continue;
                }
                if (tok.startsWith("#")) {
                    Zone z = parseId(tok.substring(1));
                    if (z == null) {
                        plugin.messages().send(sender, "command.not-found", Map.of("query", tok));
                        return;
                    }
                    parentId = z.id();
                    parentSet = true;
                    idx++;
                    continue;
                }
                if (!isNumeric(tok)) {
                    Zone z = plugin.zoneRepository().tree().resolve(tok);
                    if (z == null) {
                        plugin.messages().send(sender, "command.not-found", Map.of("query", tok));
                        return;
                    }
                    parentId = z.id();
                    parentSet = true;
                    idx++;
                    continue;
                }
            }
            break; // 数字开头 → 坐标段
        }

        if (shape == SelectionShape.SPHERE && type != ZoneType.START && type != ZoneType.END) {
            plugin.messages().send(sender, "command.invalid-shape");
            return;
        }

        UUIDHolder world = new UUIDHolder();
        ZoneSpec spec;
        int remaining = args.length - idx;
        if (shape == SelectionShape.CUBOID) {
            int[] c = null;
            if (remaining == 6) {
                c = tryParseInts(args, idx, 6);
                if (c == null) {
                    plugin.messages().send(sender, "command.invalid-hierarchy",
                            Map.of("reason", "坐标参数错误，cuboid 需要 6 个整数: <x1> <y1> <z1> <x2> <y2> <z2>"));
                    return;
                }
            } else if (remaining != 0) {
                plugin.messages().send(sender, "command.invalid-hierarchy",
                        Map.of("reason", "坐标参数数量错误，cuboid 需要 6 个整数（或留空用 WorldEdit 选区）"));
                return;
            }
            if (c == null) {
                SelectionInfo sel = plugin.worldEditHook().getCuboidSelection(player);
                if (sel == null) {
                    plugin.messages().send(sender, "command.need-selection");
                    return;
                }
                world.uid = sel.worldUid();
                c = new int[]{sel.x1(), sel.y1(), sel.z1(), sel.x2(), sel.y2(), sel.z2()};
            }
            spec = ZoneSpec.cuboid(type, name, null, null,
                    c[0], c[1], c[2], c[3], c[4], c[5]);
        } else {
            double[] c = remaining == 4 ? tryParseDoubles(args, idx, 4) : null;
            if (c == null) {
                plugin.messages().send(sender, "command.invalid-hierarchy",
                        Map.of("reason", "sphere 需要中心坐标与半径: <cx> <cy> <cz> <radius>"));
                return;
            }
            spec = ZoneSpec.sphere(type, name, null, null, c[0], c[1], c[2], c[3]);
        }

        // 解析 parent：显式 > 默认 here 推断
        if (parentHere || !parentSet) {
            parentId = inferParent(player, type);
        }
        if (type == ZoneType.GLOBAL) {
            if (parentId != null) {
                plugin.messages().send(sender, "command.invalid-hierarchy",
                        Map.of("reason", "GLOBAL 区域不能有父级"));
                return;
            }
        } else if (parentId == null) {
            plugin.messages().send(sender, "command.invalid-hierarchy",
                    Map.of("reason", "未能推断父区域（请站在目标父区域内，或用 名称/#id/'here' 显式指定）"));
            return;
        }

        UUID finalWorld = world.uid != null ? world.uid : player.getWorld().getUID();
        ZoneSpec withWorld = withWorld(spec, finalWorld, parentId);

        ValidationResult r = plugin.zoneRepository().add(withWorld, zone ->
                plugin.messages().send(sender, "command.created", Map.of(
                        "id", String.valueOf(zone.id()),
                        "name", zone.name().isEmpty() ? "(未命名)" : zone.name(),
                        "type", zone.type().name(),
                        "shape", zone.shape().name())));
        if (!r.valid()) {
            plugin.messages().send(sender, "command.invalid-hierarchy", Map.of("reason", r.reason()));
        }
    }

    /** #id 显式父区域引用（避免纯数字 id 与坐标混淆）。 */
    private Zone parseId(String digits) {
        try {
            return plugin.zoneRepository().tree().getById(Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 纯数字 token 视为坐标起点（整数或小数，可负）。 */
    private static boolean isNumeric(String tok) {
        try {
            Double.parseDouble(tok);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Integer inferParent(Player player, ZoneType type) {
        var loc = player.getLocation();
        List<Zone> chain = plugin.zoneRepository().tree().findChain(
                player.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ());
        for (Zone z : chain) {
            if (z.type().allowedChildren().contains(type)) {
                return z.id();
            }
        }
        return null;
    }

    private ZoneSpec withWorld(ZoneSpec s, java.util.UUID worldUid, Integer parentId) {
        if (s.shape() == SelectionShape.CUBOID) {
            return ZoneSpec.cuboid(s.type(), s.name(), worldUid, parentId,
                    s.x1(), s.y1(), s.z1(), s.x2(), s.y2(), s.z2());
        }
        return ZoneSpec.sphere(s.type(), s.name(), worldUid, parentId,
                s.cx(), s.cy(), s.cz(), s.radius());
    }

    private static int[] tryParseInts(String[] args, int start, int count) {
        if (start + count > args.length) {
            return null;
        }
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            try {
                out[i] = Integer.parseInt(args[start + i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }

    private static double[] tryParseDoubles(String[] args, int start, int count) {
        if (start + count > args.length) {
            return null;
        }
        double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            try {
                out[i] = Double.parseDouble(args[start + i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(List.of("global", "lobby", "level", "start", "end"), args[0]);
        }
        if (args.length == 3) {
            return filter(List.of("here", "-", "cuboid", "sphere"), args[2]);
        }
        if (args.length == 4) {
            return filter(List.of("cuboid", "sphere"), args[3]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    private static final class UUIDHolder {
        java.util.UUID uid;
    }
}
