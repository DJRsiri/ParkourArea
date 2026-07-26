package dev.aaf.parkourArea.hooks.worldedit;

import org.bukkit.entity.Player;

/** 未安装 WorldEdit/FAWE 时的退化实现。 */
public final class NoWorldEdit implements WorldEditHook {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public SelectionInfo getCuboidSelection(Player player) {
        return null;
    }
}
