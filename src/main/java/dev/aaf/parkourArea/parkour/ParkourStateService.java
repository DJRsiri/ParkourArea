package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.event.LevelCompletedEvent;
import dev.aaf.parkourArea.event.LevelStartedEvent;
import dev.aaf.parkourArea.event.PlayerZoneChangeEvent;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 跑酷状态机驱动。订阅 {@link PlayerZoneChangeEvent}，处理起点登记、离开计时、终点通关、跳关检测。
 *
 * <p>共用区域（同一物理区域兼作起点终点）：若关卡无独立 END，玩家 RUNNING 时踩 START 即算通关。</p>
 */
public final class ParkourStateService {

    private final ParkourArea plugin;
    private final LevelProgressService progressService;

    public ParkourStateService(ParkourArea plugin, LevelProgressService progressService) {
        this.plugin = plugin;
        this.progressService = progressService;
    }

    public void init() {
        plugin.eventBus().subscribe(PlayerZoneChangeEvent.class, this::onZoneChange);
    }

    private void onZoneChange(PlayerZoneChangeEvent e) {
        Player player = Bukkit.getPlayer(e.playerId());
        if (player == null) {
            return;
        }
        ParkourPlayer session = plugin.sessionService().get(e.playerId());
        if (session == null) {
            return;
        }
        Zone from = e.from();
        Zone to = e.to();

        // 离开起点 → 开始计时（AT_START → RUNNING）
        if (from != null && from.type() == ZoneType.START
                && session.phase() == PlayerPhase.AT_START
                && session.selectedLevel() != null) {
            startRunning(player, session);
        }

        if (to == null) {
            return;
        }
        if (to.type() == ZoneType.START) {
            handleStart(player, session, to);
        } else if (to.type() == ZoneType.END) {
            handleEnd(player, session, to);
        }
    }

    private void handleStart(Player player, ParkourPlayer session, Zone startZone) {
        Zone level = findLevelInChain(startZone);
        if (level == null) {
            return;
        }
        // 共用区域：玩家 RUNNING 且该关无独立 END → 踩 START 算通关
        if (session.phase() == PlayerPhase.RUNNING && !hasEndZone(level)) {
            completeLevel(player, session, level);
            return;
        }
        // 跳关检测：该关 ID 大于所属 GLOBAL 内第一个未通关关
        int globalId = plugin.zoneRepository().tree().globalOf(level.id());
        int nextExpected = progressService.firstNonCompletedLevelId(player.getUniqueId(), globalId);
        if (nextExpected != -1 && level.id() > nextExpected) {
            session.phase(PlayerPhase.INVALIDATED);
            plugin.messages().send(player, "parkour.skip-detected");
            plugin.sessionService().returnToLobby(player);
            return;
        }
        // 合法登记起点
        session.selectedLevel(level);
        session.phase(PlayerPhase.AT_START);
        session.clearCheckpoint();
        progressService.setStatus(player.getUniqueId(), level.id(), ProgressStatus.VISITED);
    }

    private void handleEnd(Player player, ParkourPlayer session, Zone endZone) {
        Zone level = findLevelInChain(endZone);
        if (level == null) {
            return;
        }
        if (session.phase() != PlayerPhase.RUNNING) {
            // 不从起点出发到达终点 → 跳关/无效
            session.phase(PlayerPhase.INVALIDATED);
            plugin.messages().send(player, "parkour.skip-detected");
            plugin.sessionService().returnToLobby(player);
            return;
        }
        if (session.selectedLevel() == null || session.selectedLevel().id() != level.id()) {
            session.phase(PlayerPhase.INVALIDATED);
            plugin.sessionService().returnToLobby(player);
            return;
        }
        completeLevel(player, session, level);
    }

    private void startRunning(Player player, ParkourPlayer session) {
        session.phase(PlayerPhase.RUNNING);
        long now = System.currentTimeMillis();
        session.levelStartedAt(now);
        plugin.eventBus().publish(new LevelStartedEvent(player.getUniqueId(), session.selectedLevel(), now));
    }

    private void completeLevel(Player player, ParkourPlayer session, Zone level) {
        long now = System.currentTimeMillis();
        long duration = now - session.levelStartedAt();
        session.phase(PlayerPhase.COMPLETED);
        session.completedDuration(duration);
        session.clearCheckpoint();
        progressService.setStatus(player.getUniqueId(), level.id(), ProgressStatus.COMPLETED);
        plugin.eventBus().publish(new LevelCompletedEvent(player.getUniqueId(), level, duration));
    }

    private Zone findLevelInChain(Zone zone) {
        Set<Integer> visited = new HashSet<>();
        Zone cur = zone;
        while (cur != null && visited.add(cur.id())) {
            if (cur.type() == ZoneType.LEVEL) {
                return cur;
            }
            cur = cur.parentId() == null ? null : plugin.zoneRepository().tree().getById(cur.parentId());
        }
        return null;
    }

    private boolean hasEndZone(Zone level) {
        List<Zone> children = plugin.zoneRepository().tree().childrenOf(level.id());
        for (Zone c : children) {
            if (c.type() == ZoneType.END) {
                return true;
            }
        }
        return false;
    }
}
