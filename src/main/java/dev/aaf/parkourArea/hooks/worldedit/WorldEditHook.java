package dev.aaf.parkourArea.hooks.worldedit;

import org.bukkit.entity.Player;

/**
 * WorldEdit/FAWE 选区钩子。软依赖：未安装时实现返回 isAvailable()=false，命令退化为要求坐标参数。
 */
public interface WorldEditHook {

    boolean isAvailable();

    /** 玩家当前 cuboid 选区；无选区或不可用时返回 null。 */
    SelectionInfo getCuboidSelection(Player player);
}
