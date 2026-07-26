package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.event.PlayerParkourTickEvent;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 防挂机服务。玩家在游玩中（AT_START/RUNNING）超过 {@code anti-idle-seconds} 无位置/视角变化时，
 * 成绩作废并传回大厅。
 *
 * <p>位置/视角快照由 {@code PlayerRegionTracker} 每 tick 更新到 session（updateMovement）。</p>
 */
public final class AntiIdleService {

    private final ParkourArea plugin;

    public AntiIdleService(ParkourArea plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.eventBus().subscribe(PlayerParkourTickEvent.class, this::onTick);
    }

    private void onTick(PlayerParkourTickEvent e) {
        ParkourPlayer session = e.session();
        if (session.phase() != PlayerPhase.RUNNING && session.phase() != PlayerPhase.AT_START) {
            return;
        }
        long idleMs = System.currentTimeMillis() - session.lastMoveAt();
        long thresholdMs = plugin.configService().settings().antiIdleSeconds() * 1000L;
        if (idleMs <= thresholdMs) {
            return;
        }
        session.phase(PlayerPhase.INVALIDATED);
        Player player = Bukkit.getPlayer(e.playerId());
        if (player != null) {
            plugin.messages().send(player, "parkour.idle-invalidated");
            plugin.sessionService().returnToLobby(player);
        }
    }
}
