package dev.aaf.parkourArea.visibility;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.event.ParkourMarkedEvent;
import dev.aaf.parkourArea.event.ParkourUnmarkedEvent;
import dev.aaf.parkourArea.event.PlayerZoneChangeEvent;
import dev.aaf.parkourArea.hooks.protocollib.VisibilityHook;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.VisibilityMode;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家可见性服务。管理每玩家在「关卡区域内」对其他跑酷玩家的可见性挡位。
 *
 * <p>事件驱动刷新（无每 tick 任务）：zone change / marked / unmarked 触发增量更新。
 * viewer 视角下的实体操作（hide/show/发包）切到 viewer 的 entity 线程。</p>
 */
public final class VisibilityService {

    private final ParkourArea plugin;
    /** 每个 viewer 视角下曾修改过的 target 集合，用于精准还原。 */
    private final Map<UUID, Set<UUID>> modifiedTargets = new HashMap<>();

    public VisibilityService(ParkourArea plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.eventBus().subscribe(PlayerZoneChangeEvent.class, this::onZoneChange);
        plugin.eventBus().subscribe(ParkourMarkedEvent.class, this::onMarked);
        plugin.eventBus().subscribe(ParkourUnmarkedEvent.class, this::onUnmarked);
    }

    /** 工具右键：循环切换挡位并刷新 viewer 视角。调用方可能不在 global 线程，内部切回。 */
    public void cycleMode(Player viewer) {
        UUID uid = viewer.getUniqueId();
        plugin.scheduler().runGlobal(() -> {
            ParkourPlayer s = plugin.sessionService().get(uid);
            if (s == null) {
                return;
            }
            VisibilityMode next = s.visibilityMode().next();
            s.visibilityMode(next);
            if (plugin.preferenceService() != null) {
                plugin.preferenceService().saveAsync(uid, s);
            }
            refreshViewer(uid);
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                plugin.messages().send(p, "parkour.visibility-" + next.name().toLowerCase());
            }
        });
    }

    private void onZoneChange(PlayerZoneChangeEvent e) {
        ParkourPlayer s = plugin.sessionService().get(e.playerId());
        if (s == null) {
            return;
        }
        boolean wasLevel = isLevelZone(e.from());
        boolean isLevel = isLevelZone(e.to());
        if (!wasLevel && isLevel) {
            // 作为 viewer 进入 LEVEL：全量刷新
            refreshViewer(e.playerId());
            // 作为 target 进入 LEVEL：对其他 viewer 增量施加
            addTargetToViewers(e.playerId());
        } else if (wasLevel && !isLevel) {
            // 作为 viewer 离开 LEVEL：还原视角
            resetViewer(e.playerId());
            // 作为 target 离开 LEVEL：对其他 viewer 还原
            removeTargetFromViewers(e.playerId());
        }
    }

    private void onMarked(ParkourMarkedEvent e) {
        ParkourPlayer s = plugin.sessionService().get(e.playerId());
        if (s == null || !isLevelParkourPlayer(s)) {
            return;
        }
        refreshViewer(e.playerId());
        addTargetToViewers(e.playerId());
    }

    private void onUnmarked(ParkourUnmarkedEvent e) {
        resetViewer(e.playerId());
        removeTargetFromViewers(e.playerId());
    }

    /** 全量刷新 viewer 视角：先还原旧的，再按当前 mode 施加 LEVEL 内所有 target。 */
    private void refreshViewer(UUID viewer) {
        resetViewer(viewer);
        ParkourPlayer s = plugin.sessionService().get(viewer);
        if (s == null || s.visibilityMode() == VisibilityMode.FULL) {
            return;
        }
        if (!isLevelParkourPlayer(s)) {
            return;
        }
        VisibilityMode mode = s.visibilityMode();
        for (UUID target : getLevelPlayers()) {
            if (target.equals(viewer)) {
                continue;
            }
            applyMode(viewer, target, mode);
            modifiedTargets.computeIfAbsent(viewer, k -> new HashSet<>()).add(target);
        }
    }

    /** 还原 viewer 视角下所有曾修改的 target 到完全可见。 */
    private void resetViewer(UUID viewer) {
        Set<UUID> targets = modifiedTargets.remove(viewer);
        if (targets == null) {
            return;
        }
        Player v = Bukkit.getPlayer(viewer);
        if (v == null) {
            return;
        }
        VisibilityHook hook = plugin.visibilityHook();
        for (UUID target : targets) {
            Player t = Bukkit.getPlayer(target);
            if (t != null) {
                plugin.scheduler().runEntity(v, vv -> hook.resetScale(vv, t), () -> {});
            }
        }
    }

    /** target 进入 LEVEL：对每个在 LEVEL 的 viewer（非 FULL 模式）施加该 target。 */
    private void addTargetToViewers(UUID target) {
        for (UUID viewer : getLevelPlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            ParkourPlayer vs = plugin.sessionService().get(viewer);
            if (vs == null || vs.visibilityMode() == VisibilityMode.FULL) {
                continue;
            }
            applyMode(viewer, target, vs.visibilityMode());
            modifiedTargets.computeIfAbsent(viewer, k -> new HashSet<>()).add(target);
        }
    }

    /** target 离开 LEVEL：对每个曾修改该 target 的 viewer 还原。 */
    private void removeTargetFromViewers(UUID target) {
        for (UUID viewer : new HashSet<>(modifiedTargets.keySet())) {
            Set<UUID> set = modifiedTargets.get(viewer);
            if (set == null || !set.remove(target)) {
                continue;
            }
            Player v = Bukkit.getPlayer(viewer);
            Player t = Bukkit.getPlayer(target);
            if (v != null && t != null) {
                VisibilityHook hook = plugin.visibilityHook();
                plugin.scheduler().runEntity(v, vv -> hook.resetScale(vv, t), () -> {});
            }
        }
    }

    private void applyMode(UUID viewer, UUID target, VisibilityMode mode) {
        Player v = Bukkit.getPlayer(viewer);
        Player t = Bukkit.getPlayer(target);
        if (v == null || t == null) {
            return;
        }
        VisibilityHook hook = plugin.visibilityHook();
        plugin.scheduler().runEntity(v, vv -> {
            switch (mode) {
                case FULL -> hook.resetScale(vv, t);
                case HIDDEN -> vv.hidePlayer(plugin, t);
                case HALF -> hook.applyScale(vv, t, 0.5f);
                case TENTH -> hook.applyScale(vv, t, 0.1f);
            }
        }, () -> {});
    }

    private boolean isLevelParkourPlayer(ParkourPlayer s) {
        return s != null && isLevelZone(s.currentZone());
    }

    private boolean isLevelZone(Zone zone) {
        return zone != null && findAncestorByType(zone, ZoneType.LEVEL) != null;
    }

    private Zone findAncestorByType(Zone zone, ZoneType type) {
        Set<Integer> visited = new HashSet<>();
        Zone cur = zone;
        while (cur != null && visited.add(cur.id())) {
            if (cur.type() == type) {
                return cur;
            }
            cur = cur.parentId() == null ? null : plugin.zoneRepository().tree().getById(cur.parentId());
        }
        return null;
    }

    private List<UUID> getLevelPlayers() {
        List<UUID> out = new ArrayList<>();
        for (ParkourPlayer s : plugin.sessionService().all().values()) {
            if (isLevelParkourPlayer(s)) {
                out.add(s.uuid());
            }
        }
        return out;
    }

    /** 玩家断线：清理该 viewer 状态，并从其他 viewer 的 modifiedTargets 移除（target 离线自动隐藏）。 */
    public void onDisconnect(UUID viewer) {
        modifiedTargets.remove(viewer);
        for (Set<UUID> set : modifiedTargets.values()) {
            set.remove(viewer);
        }
    }
}
