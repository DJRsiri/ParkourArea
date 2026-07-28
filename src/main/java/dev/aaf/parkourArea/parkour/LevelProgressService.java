package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.persistence.PlayerProgressDao;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 玩家通关进度服务：内存缓存 + 异步落库。
 *
 * <p>玩家进入全局区域时 {@link #loadAsync} 异步加载；运行时 getStatus 走内存（global region 线程，无阻塞）。
 * 关卡按所属 GLOBAL 过滤，同 GLOBAL 内按区域数字 ID 升序。</p>
 */
public final class LevelProgressService {

    private final ParkourArea plugin;
    private final PlayerProgressDao dao;
    private final Map<UUID, Map<Integer, ProgressStatus>> cache = new HashMap<>();
    private final Set<UUID> loaded = new HashSet<>();

    public LevelProgressService(ParkourArea plugin) {
        this.plugin = plugin;
        this.dao = plugin.progressDao();
    }

    public void loadAsync(UUID uuid) {
        plugin.scheduler().runAsync(() -> {
            Map<Integer, ProgressStatus> statuses;
            try {
                statuses = dao.getAllStatuses(uuid);
            } catch (SQLException e) {
                plugin.getLogger().warning("加载玩家进度失败: " + e.getMessage());
                statuses = new HashMap<>();
            }
            Map<Integer, ProgressStatus> copy = new HashMap<>(statuses);
            plugin.scheduler().runGlobal(() -> {
                cache.put(uuid, copy);
                loaded.add(uuid);
            });
        });
    }

    /**
     * 同步加载（在 global region 线程调用，阻塞 DB 一次）。
     * 用于玩家进入全局区域时立即加载，避免后续跳关判定读到空缓存。SQLite 本地查询毫秒级，可接受。
     */
    public void loadSync(UUID uuid) {
        try {
            Map<Integer, ProgressStatus> statuses = dao.getAllStatuses(uuid);
            cache.put(uuid, new HashMap<>(statuses));
            loaded.add(uuid);
        } catch (SQLException e) {
            plugin.getLogger().warning("加载玩家进度失败: " + e.getMessage());
            cache.computeIfAbsent(uuid, k -> new HashMap<>());
            loaded.add(uuid);
        }
    }

    public boolean isLoaded(UUID uuid) {
        return loaded.contains(uuid);
    }

    public ProgressStatus getStatus(UUID uuid, int levelId) {
        Map<Integer, ProgressStatus> m = cache.get(uuid);
        return m == null ? ProgressStatus.NONE : m.getOrDefault(levelId, ProgressStatus.NONE);
    }

    public Map<Integer, ProgressStatus> getStatuses(UUID uuid) {
        return cache.getOrDefault(uuid, Collections.emptyMap());
    }

    public void setStatus(UUID uuid, int levelId, ProgressStatus status) {
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(levelId, status);
        plugin.scheduler().runAsync(() -> {
            try {
                dao.setStatus(uuid, levelId, status, System.currentTimeMillis());
            } catch (SQLException e) {
                plugin.getLogger().warning("保存玩家进度失败: " + e.getMessage());
            }
        });
    }

    /** 删档：清除玩家在某关的进度与计时/最佳记录。 */
    public void deleteProgressAsync(UUID uuid, int levelId) {
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(levelId, ProgressStatus.NONE);
        plugin.scheduler().runAsync(() -> {
            try {
                dao.clearProgress(uuid, levelId);
                dao.clearStats(uuid, levelId);
            } catch (SQLException e) {
                plugin.getLogger().warning("删档失败: " + e.getMessage());
            }
        });
    }

    /** 指定 GLOBAL 下按关卡 ID 升序的第一个未 COMPLETED 关卡 ID；全通关或无关卡返回 -1。 */
    public int firstNonCompletedLevelId(UUID uuid, int globalId) {
        List<Integer> ids = sortedLevelIds(globalId);
        for (int id : ids) {
            if (getStatus(uuid, id) != ProgressStatus.COMPLETED) {
                return id;
            }
        }
        return -1;
    }

    /** 指定 GLOBAL 的直接 LEVEL 子区域 id 升序列表。 */
    public List<Integer> sortedLevelIds(int globalId) {
        return plugin.zoneRepository().tree().childrenOf(globalId).stream()
                .filter(z -> z.type() == ZoneType.LEVEL)
                .map(Zone::id)
                .sorted()
                .collect(Collectors.toList());
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
        loaded.remove(uuid);
    }
}
