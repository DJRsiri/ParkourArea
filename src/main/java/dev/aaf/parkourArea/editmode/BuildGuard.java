package dev.aaf.parkourArea.editmode;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * 编辑模式守卫：非编辑模式玩家在全局区域内放/破坏方块被取消（但仍可交互按钮、箱子等）。
 */
public final class BuildGuard implements Listener {

    private final ParkourArea plugin;

    public BuildGuard(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        guard(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        guard(event.getPlayer(), event.getBlock(), event);
    }

    private void guard(Player player, Block block, Cancellable event) {
        if (plugin.editModeService().isEditMode(player)) {
            return; // 编辑模式允许
        }
        if (!isInGlobal(block)) {
            return; // 不在跑酷全局区域内，放行
        }
        event.setCancelled(true);
        plugin.messages().send(player, "command.edit-mode-block");
    }

    private boolean isInGlobal(Block block) {
        Zone z = plugin.zoneRepository().tree().findMostSpecific(
                block.getWorld().getUID(), block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
        while (z != null) {
            if (z.type() == ZoneType.GLOBAL) {
                return true;
            }
            z = z.parentId() == null ? null : plugin.zoneRepository().tree().getById(z.parentId());
        }
        return false;
    }
}
