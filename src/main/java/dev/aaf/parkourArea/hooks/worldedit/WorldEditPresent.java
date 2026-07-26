package dev.aaf.parkourArea.hooks.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * WorldEdit/FAWE 选区读取（FAWE 兼容 WorldEdit API，故同一实现覆盖两者）。
 * 通过 Bukkit.getPluginManager() 检测，软依赖惰性解析。
 */
public final class WorldEditPresent implements WorldEditHook {

    private final WorldEditPlugin wePlugin;

    public WorldEditPresent(WorldEditPlugin wePlugin) {
        this.wePlugin = wePlugin;
    }

    /** 检测是否安装了 WorldEdit 或 FAWE；是则返回实例，否则 null。 */
    public static WorldEditPresent tryCreate() {
        var plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (plugin instanceof WorldEditPlugin we) {
            return new WorldEditPresent(we);
        }
        return null;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public SelectionInfo getCuboidSelection(Player player) {
        try {
            var session = wePlugin.getSession(player);
            var selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            if (selection == null) {
                return null;
            }
            var min = selection.getMinimumPoint();
            var max = selection.getMaximumPoint();
            return new SelectionInfo(player.getWorld().getUID(),
                    min.x(), min.y(), min.z(),
                    max.x(), max.y(), max.z());
        } catch (Throwable t) {
            return null;
        }
    }
}
