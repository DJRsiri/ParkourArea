package dev.aaf.parkourArea.util;

import dev.aaf.parkourArea.zone.SelectionShape;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneSpawn;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** Bukkit 坐标/方块工具。 */
public final class Locations {

    private Locations() {}

    /**
     * 玩家脚下方块（y-0.1 检测）。
     *
     * <p>需求约定：「判定是否踩上去通关检测玩家 y轴~-0.1 的方块确定」。
     * Bukkit 的 {@link Player#getLocation()} 给的是脚部位置，再减 0.1 即得到脚下方块。</p>
     */
    public static Block blockBelowFeet(Player player) {
        return blockBelowFeet(player.getLocation());
    }

    public static Block blockBelowFeet(Location feet) {
        return feet.clone().subtract(0, 0.1, 0).getBlock();
    }

    /** 玩家所站方块（脚部位置的 block）。 */
    public static Block feetBlock(Player player) {
        return player.getLocation().getBlock();
    }

    /** 同一世界下的方块读取（用于在区域线程读取脚下方块类型）。 */
    public static Block blockAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z);
    }

    /** 区域中心的 x 坐标（CUBOID 取方块区间中心 +0.5；SPHERE 取 center）。 */
    public static double centerX(Zone z) {
        return z.shape() == SelectionShape.CUBOID
                ? (z.minX() + z.maxX()) / 2.0 + 0.5
                : z.centerX();
    }

    /** 区域中心的 z 坐标。 */
    public static double centerZ(Zone z) {
        return z.shape() == SelectionShape.CUBOID
                ? (z.minZ() + z.maxZ()) / 2.0 + 0.5
                : z.centerZ();
    }

    /**
     * 从世界最高建筑高度向下找第一个非空气方块，返回其上一格（玩家脚部 y）。
     * 全列皆空气时返回世界底部 y（极端情况，跑酷场景不会发生）。
     */
    public static int highestNonAirY(World world, int x, int z) {
        int y = world.getMaxHeight() - 1;
        int minY = world.getMinHeight();
        while (y >= minY && world.getBlockAt(x, y, z).getType().isAir()) {
            y--;
        }
        return y + 1;
    }

    /**
     * 解析传送目标 Location。{@code posZone} 提供坐标（可用 spawn.x/y/z 覆盖），
     * {@code orientZone} 提供朝向（可用 spawn.yaw/pitch 覆盖）。各字段逐级 fallback：
     * spawn 显式值 → 区域几何默认（坐标=中心 + 该列最高非空气方块上一格，朝向=0/0）。
     *
     * <p>典型：大厅 tp 传 {@code (lobby, lobby)}；起点 tp 传 {@code (start, level)}。</p>
     */
    public static Location teleportLocation(World world, Zone posZone, Zone orientZone) {
        ZoneSpawn ps = posZone.spawn();
        ZoneSpawn os = orientZone == null ? null : orientZone.spawn();
        double cx = ps != null && ps.x() != null ? ps.x() : centerX(posZone);
        double cz = ps != null && ps.z() != null ? ps.z() : centerZ(posZone);
        double cy = ps != null && ps.y() != null
                ? ps.y()
                : highestNonAirY(world, (int) Math.floor(cx), (int) Math.floor(cz));
        float yaw = os != null && os.yaw() != null ? os.yaw().floatValue()
                : (ps != null && ps.yaw() != null ? ps.yaw().floatValue() : 0f);
        float pitch = os != null && os.pitch() != null ? os.pitch().floatValue()
                : (ps != null && ps.pitch() != null ? ps.pitch().floatValue() : 0f);
        return new Location(world, cx, cy, cz, yaw, pitch);
    }
}
