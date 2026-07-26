package dev.aaf.parkourArea.util;

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
}
