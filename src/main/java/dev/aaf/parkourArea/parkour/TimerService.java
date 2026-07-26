package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.event.LevelCompletedEvent;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 关卡计时服务：订阅 {@link LevelCompletedEvent}，把用时写入前 N 次记录与最佳记录（异步落库）。
 * 同时维护最佳用时内存缓存供 actionbar 渲染快速读取。
 */
public final class TimerService {

    private final ParkourArea plugin;
    private final Map<UUID, Map<Integer, Long>> bestCache = new HashMap<>();

    public TimerService(ParkourArea plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.eventBus().subscribe(LevelCompletedEvent.class, this::onComplete);
    }

    public void loadBestAsync(UUID uuid) {
        plugin.scheduler().runAsync(() -> {
            Map<Integer, Long> bests;
            try {
                bests = plugin.bestDao().getAllBests(uuid);
            } catch (SQLException e) {
                plugin.getLogger().warning("加载最佳用时失败: " + e.getMessage());
                bests = new HashMap<>();
            }
            Map<Integer, Long> copy = new HashMap<>(bests);
            plugin.scheduler().runGlobal(() -> bestCache.put(uuid, copy));
        });
    }

    /** 同步加载最佳用时（global region，阻塞 DB 一次）。 */
    public void loadBestSync(UUID uuid) {
        try {
            bestCache.put(uuid, new HashMap<>(plugin.bestDao().getAllBests(uuid)));
        } catch (SQLException e) {
            plugin.getLogger().warning("加载最佳用时失败: " + e.getMessage());
            bestCache.computeIfAbsent(uuid, k -> new HashMap<>());
        }
    }

    private void onComplete(LevelCompletedEvent e) {
        final long duration = e.durationMillis();
        final int levelId = e.level().id();
        plugin.scheduler().runAsync(() -> {
            try {
                long now = System.currentTimeMillis();
                plugin.timeDao().addTime(e.playerId(), levelId, duration, now);
                boolean updated = plugin.bestDao().updateBestIfBetter(e.playerId(), levelId, duration, now);
                if (updated) {
                    plugin.scheduler().runGlobal(() ->
                            bestCache.computeIfAbsent(e.playerId(), k -> new HashMap<>()).put(levelId, duration));
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("保存通关计时失败: " + ex.getMessage());
            }
        });
    }

    public Long getBest(UUID uuid, int levelId) {
        Map<Integer, Long> m = bestCache.get(uuid);
        return m == null ? null : m.get(levelId);
    }

    public Map<Integer, Long> getBests(UUID uuid) {
        return bestCache.getOrDefault(uuid, Collections.emptyMap());
    }

    public void remove(UUID uuid) {
        bestCache.remove(uuid);
    }
}
