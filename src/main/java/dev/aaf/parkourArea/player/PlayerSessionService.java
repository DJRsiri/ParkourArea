package dev.aaf.parkourArea.player;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.event.EventBus;
import dev.aaf.parkourArea.event.ParkourMarkedEvent;
import dev.aaf.parkourArea.event.ParkourUnmarkedEvent;
import dev.aaf.parkourArea.event.PlayerZoneChangeEvent;
import dev.aaf.parkourArea.hotbar.HotbarService;
import dev.aaf.parkourArea.persistence.Preference;
import dev.aaf.parkourArea.util.Locations;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneRepository;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 跑酷玩家会话协调中心。订阅 {@link PlayerZoneChangeEvent}，处理进出全局区域的标记/取消。
 *
 * <p><b>线程约定</b>：所有方法假定在 global region 线程调用（由 RegionTracker 每 2tick 检测驱动）。</p>
 */
public final class PlayerSessionService {

    private final ParkourArea plugin;
    private final EventBus eventBus;
    private final ZoneRepository zoneRepository;
    private final HotbarService hotbarService;
    private final Map<UUID, ParkourPlayer> sessions = new HashMap<>();

    public PlayerSessionService(ParkourArea plugin, EventBus eventBus,
                                ZoneRepository zoneRepository, HotbarService hotbarService) {
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.zoneRepository = zoneRepository;
        this.hotbarService = hotbarService;
    }

    public void init() {
        eventBus.subscribe(PlayerZoneChangeEvent.class, this::onZoneChange);
    }

    private void onZoneChange(PlayerZoneChangeEvent e) {
        Player player = Bukkit.getPlayer(e.playerId());
        if (player == null) {
            return;
        }
        ParkourPlayer session = sessions.get(e.playerId());
        boolean inGlobal = isInGlobal(e.to());

        if (inGlobal && session == null) {
            tryMark(player);
        } else if (!inGlobal && session != null) {
            unmark(player, UnmarkReason.LEFT_ZONE);
        } else if (session != null && e.to() != null) {
            session.currentZone(e.to());
        }
    }

    /** zone 是否处于某 GLOBAL 区域内（沿 parent 链向上查找）。 */
    private boolean isInGlobal(Zone zone) {
        if (zone == null) {
            return false;
        }
        Zone cur = zone;
        Set<Integer> visited = new HashSet<>();
        while (cur != null && visited.add(cur.id())) {
            if (cur.type() == ZoneType.GLOBAL) {
                return true;
            }
            cur = cur.parentId() == null ? null : zoneRepository.tree().getById(cur.parentId());
        }
        return false;
    }

    private void tryMark(Player player) {
        // 必须生存/冒险模式才标记
        if (!plugin.configService().settings().requiresGameMode(player.getGameMode())) {
            return;
        }
        // 编辑模式不参与跑酷玩法
        if (plugin.editModeService().isEditMode(player)) {
            return;
        }
        ParkourPlayer session = new ParkourPlayer(player.getUniqueId());
        session.savedState(PlayerState.snapshot(player));
        session.phase(PlayerPhase.IN_LOBBY);
        var loc = player.getLocation();
        session.initPosition(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                System.currentTimeMillis());
        session.currentZone(zoneRepository.tree().findMostSpecific(
                player.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ()));
        // 加载玩家进度与最佳用时缓存（global region 同步，SQLite 毫秒级）
        if (plugin.progressService() != null) {
            plugin.progressService().loadSync(player.getUniqueId());
        }
        if (plugin.timerService() != null) {
            plugin.timerService().loadBestSync(player.getUniqueId());
        }
        loadPreference(player, session);
        sessions.put(player.getUniqueId(), session);
        hotbarService.applyTools(player);
        plugin.messages().send(player, "parkour.marked");
        eventBus.publish(new ParkourMarkedEvent(player.getUniqueId()));
    }

    /** 加载玩家偏好（音效三层 + 可见性挡位）覆盖 session 默认值；无记录用 Settings 默认。 */
    private void loadPreference(Player player, ParkourPlayer session) {
        if (plugin.preferenceDao() == null) {
            return;
        }
        var settings = plugin.configService().settings();
        Preference fallback = new Preference(settings.soundEnabled(), settings.checkpointSoundEnabled(),
                settings.blockSoundEnabled(), settings.defaultVisibilityMode());
        try {
            Preference pref = plugin.preferenceDao().getOrDefault(player.getUniqueId(), fallback);
            session.soundEnabled(pref.sound());
            session.checkpointSoundEnabled(pref.checkpoint());
            session.blockSoundEnabled(pref.block());
            session.visibilityMode(pref.mode());
        } catch (Exception e) {
            plugin.getLogger().warning("加载玩家偏好失败: " + e.getMessage());
        }
    }

    public void unmark(Player player, UnmarkReason reason) {
        ParkourPlayer session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.savedState() != null) {
            try {
                session.savedState().restore(player);
            } catch (Exception e) {
                // restore 失败不阻塞会话移除，避免玩家卡死在跑酷状态
                plugin.getLogger().warning("恢复玩家状态失败: " + e.getMessage());
            }
        }
        if (plugin.progressService() != null) {
            plugin.progressService().remove(player.getUniqueId());
        }
        if (plugin.timerService() != null) {
            plugin.timerService().remove(player.getUniqueId());
        }
        if (plugin.blockCommandService() != null) {
            plugin.blockCommandService().clear(player.getUniqueId());
        }
        String path = switch (reason) {
            case LEFT_ZONE -> "parkour.unmarked";
            case GAMEMODE -> "parkour.unmarked-gamemode";
            case EDIT_MODE -> "parkour.unmarked-editmode";
        };
        plugin.messages().send(player, path);
        eventBus.publish(new ParkourUnmarkedEvent(player.getUniqueId()));
    }

    /**
     * 每 tick 前提条件对账（由 PlayerRegionTracker 调用）：
     * 已标记但条件失效（编辑模式或游戏模式不合规）→ 取消标记；
     * 未标记但处于 GLOBAL 内且条件满足 → 补标记。
     */
    public void reconcile(Player player, Zone currentZone) {
        ParkourPlayer session = sessions.get(player.getUniqueId());
        boolean editMode = plugin.editModeService().isEditMode(player);
        boolean modeOk = plugin.configService().settings().requiresGameMode(player.getGameMode());
        if (session != null) {
            if (editMode) {
                unmark(player, UnmarkReason.EDIT_MODE);
            } else if (!modeOk) {
                unmark(player, UnmarkReason.GAMEMODE);
            }
        } else if (modeOk && !editMode && isInGlobal(currentZone)) {
            tryMark(player);
        }
    }

    /** 每 tick 更新玩家位置/视角快照（防挂机用）。 */
    public void updateMovement(Player player, ParkourPlayer session) {
        var loc = player.getLocation();
        session.updateMovement(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                System.currentTimeMillis(), 0.1,
                plugin.configService().settings().antiIdleRotationThresholdDeg());
    }

    public ParkourPlayer get(UUID uuid) {
        return sessions.get(uuid);
    }

    public boolean isParkour(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public Map<UUID, ParkourPlayer> all() {
        return Collections.unmodifiableMap(sessions);
    }

    /** 玩家断线时调用（player 仍有效，恢复状态后移除会话）。 */
    public void onDisconnect(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        ParkourPlayer session = sessions.get(uuid);
        if (player != null && session != null && session.savedState() != null) {
            session.savedState().restore(player);
        }
        if (plugin.progressService() != null) {
            plugin.progressService().remove(uuid);
        }
        if (plugin.timerService() != null) {
            plugin.timerService().remove(uuid);
        }
        if (plugin.blockCommandService() != null) {
            plugin.blockCommandService().clear(uuid);
        }
        sessions.remove(uuid);
    }

    /** 服务器关闭时恢复所有在线跑酷玩家的原始状态。 */
    public void shutdown() {
        for (UUID uuid : new HashSet<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            ParkourPlayer session = sessions.get(uuid);
            if (player != null && session != null && session.savedState() != null) {
                session.savedState().restore(player);
            }
        }
        sessions.clear();
    }

    /** 返回大厅（跳关/防挂机/重玩失败时）。假设在 global region 线程调用；传送切 entity 线程。 */
    public void returnToLobby(Player player) {
        ParkourPlayer session = get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.phase(PlayerPhase.IN_LOBBY);
        session.selectedLevel(null);
        session.clearCheckpoint();
        Zone lobby = findLobbyAround(player);
        plugin.scheduler().runEntity(player, p -> {
            if (lobby != null) {
                World world = lobby.worldUid() != null
                        ? Bukkit.getWorld(lobby.worldUid()) : p.getWorld();
                if (world != null) {
                    // 坐标/朝向取 LOBBY spawn（坐标缺省取区域中心+最高非空气方块）；
                    // 未指定朝向字段按配置保留玩家当前朝向或回落 0/0
                    p.teleport(Locations.teleportLocation(world, lobby, lobby,
                            plugin.configService().settings().teleportKeepRotation(), p.getLocation()));
                }
            }
            plugin.messages().send(p, "parkour.teleported-lobby");
        }, () -> {});
    }

    private Zone findLobbyAround(Player player) {
        var loc = player.getLocation();
        List<Zone> chain = zoneRepository.tree().findChain(
                player.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ());
        for (Zone z : chain) {
            if (z.type() == ZoneType.LOBBY) {
                return z;
            }
        }
        // 若玩家当前不在大厅，尝试找所在 GLOBAL 下的任意 LOBBY
        for (Zone z : chain) {
            if (z.type() == ZoneType.GLOBAL) {
                for (Zone child : zoneRepository.tree().childrenOf(z.id())) {
                    if (child.type() == ZoneType.LOBBY) {
                        return child;
                    }
                }
            }
        }
        return null;
    }
}
