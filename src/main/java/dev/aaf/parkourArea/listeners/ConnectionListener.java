package dev.aaf.parkourArea.listeners;

import dev.aaf.parkourArea.ParkourArea;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** 玩家断线时清理会话（恢复状态）与区域跟踪记录。 */
public final class ConnectionListener implements Listener {

    private final ParkourArea plugin;

    public ConnectionListener(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.sessionService().onDisconnect(event.getPlayer().getUniqueId());
        if (plugin.visibilityService() != null) {
            plugin.visibilityService().onDisconnect(event.getPlayer().getUniqueId());
        }
        plugin.regionTracker().remove(event.getPlayer().getUniqueId());
    }
}
