package dev.aaf.parkourArea.listeners;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.command.Permission;
import dev.aaf.parkourArea.config.ConfigVersionChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

/** 玩家断线时清理会话（恢复状态）与区域跟踪记录。 */
public final class ConnectionListener implements Listener {

    private final ParkourArea plugin;

    public ConnectionListener(ParkourArea plugin) {
        this.plugin = plugin;
    }

    /** 管理员进服时若配置文件版本仍过旧，逐文件提示一次。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission(Permission.ADMIN)) {
            return;
        }
        if (plugin.configService().versionChecker() == null) {
            return;
        }
        for (ConfigVersionChecker.Outdated o : plugin.configService().versionChecker().outdated()) {
            plugin.messages().send(p, "command.config-outdated", Map.of(
                    "file", o.file(),
                    "current", String.valueOf(o.current()),
                    "expected", String.valueOf(o.expected())));
        }
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
