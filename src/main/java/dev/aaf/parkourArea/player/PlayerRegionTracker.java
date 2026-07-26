package dev.aaf.parkourArea.player;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.concurrency.Scheduler;
import dev.aaf.parkourArea.concurrency.TaskHandle;
import dev.aaf.parkourArea.event.EventBus;
import dev.aaf.parkourArea.event.PlayerZoneChangeEvent;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 每 2tick（detect-interval-ticks）在 global region 线程上扫描所有在线玩家，
 * 检测其最具体区域是否变化，发布 {@link PlayerZoneChangeEvent}，并更新防挂机位置快照。
 */
public final class PlayerRegionTracker {

    private final ParkourArea plugin;
    private final Scheduler scheduler;
    private final EventBus eventBus;
    private final ZoneRepository zoneRepository;
    private final PlayerSessionService sessionService;
    private final Map<UUID, Zone> lastZones = new HashMap<>();
    private TaskHandle task;

    public PlayerRegionTracker(ParkourArea plugin, PlayerSessionService sessionService) {
        this.plugin = plugin;
        this.scheduler = plugin.scheduler();
        this.eventBus = plugin.eventBus();
        this.zoneRepository = plugin.zoneRepository();
        this.sessionService = sessionService;
    }

    public void start() {
        int interval = plugin.configService().settings().detectIntervalTicks();
        task = scheduler.runGlobalAtFixedRate(this::tick, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick(TaskHandle handle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uid = player.getUniqueId();
            var loc = player.getLocation();
            if (loc.getWorld() == null) {
                continue;
            }
            Zone to = zoneRepository.tree().findMostSpecific(
                    loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ());
            Zone from = lastZones.get(uid);
            if (from == null ? to != null : !from.equals(to)) {
                lastZones.put(uid, to);
                eventBus.publish(new PlayerZoneChangeEvent(uid, from, to));
            }
            // 更新防挂机位置快照 + 发布跑酷 tick 事件（仅对已标记的跑酷玩家）
            ParkourPlayer session = sessionService.get(uid);
            if (session != null) {
                sessionService.updateMovement(player, session);
                eventBus.publish(new dev.aaf.parkourArea.event.PlayerParkourTickEvent(uid, session));
            }
        }
    }

    public void remove(UUID uid) {
        lastZones.remove(uid);
    }
}
