package dev.aaf.parkourArea.concurrency;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 直接桥接 paper-api 暴露的四套调度器（Paper 上 shim 到主线程，Folia 上跑真实区域线程）。
 * 不引入第三方 FoliaLib，零反射。
 */
public final class UnifiedScheduler implements Scheduler {

    private final Plugin plugin;

    public UnifiedScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    // ---- 实体调度器（玩家状态变更） ----

    @Override
    public void runEntity(Player player, Consumer<Player> action, Runnable retired) {
        player.getScheduler().execute(plugin, () -> action.accept(player),
                retired == null ? () -> {} : retired, 1);
    }

    @Override
    public void runEntityDelayed(Player player, Consumer<Player> action, Runnable retired, long delayTicks) {
        player.getScheduler().execute(plugin, () -> action.accept(player),
                retired == null ? () -> {} : retired, delayTicks);
    }

    @Override
    public TaskHandle runEntityAtFixedRate(Player player, Consumer<Player> action, Runnable retired,
                                           long initialDelayTicks, long periodTicks) {
        final TaskHandle[] holder = new TaskHandle[1];
        ScheduledTask st = player.getScheduler().runAtFixedRate(plugin,
                t -> action.accept(player),
                retired == null ? () -> {} : retired,
                initialDelayTicks, periodTicks);
        holder[0] = new TaskHandle(st);
        return holder[0];
    }

    // ---- 区域调度器（位置相关读取） ----

    @Override
    public void runAtLocation(Location location, Runnable action) {
        Bukkit.getRegionScheduler().execute(plugin, location, action);
    }

    // ---- 全局区域调度器（全服周期任务） ----

    @Override
    public void runGlobal(Runnable action) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, action);
    }

    @Override
    public void runGlobalDelayed(Runnable action, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> action.run(), delayTicks);
    }

    @Override
    public TaskHandle runGlobalAtFixedRate(Consumer<TaskHandle> action, long initialDelayTicks, long periodTicks) {
        final TaskHandle[] holder = new TaskHandle[1];
        ScheduledTask st = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                t -> action.accept(holder[0]), initialDelayTicks, periodTicks);
        holder[0] = new TaskHandle(st);
        return holder[0];
    }

    // ---- 异步调度器（阻塞 IO / CPU） ----

    @Override
    public void runAsync(Runnable action) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> action.run());
    }

    @Override
    public void runAsyncDelayed(Runnable action, long delay, TimeUnit unit) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, t -> action.run(), delay, unit);
    }

    @Override
    public TaskHandle runAsyncAtFixedRate(Consumer<TaskHandle> action, long initialDelay, long period, TimeUnit unit) {
        final TaskHandle[] holder = new TaskHandle[1];
        ScheduledTask st = Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                t -> action.accept(holder[0]), initialDelay, period, unit);
        holder[0] = new TaskHandle(st);
        return holder[0];
    }
}
