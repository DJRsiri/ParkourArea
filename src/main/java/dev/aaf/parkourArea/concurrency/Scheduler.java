package dev.aaf.parkourArea.concurrency;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 平台无关的任务调度门面。
 *
 * <p>实现 {@link UnifiedScheduler} 直接桥接 paper-api 暴露的 region/async/global/entity 调度器，
 * 因此一套代码同时运行于 Paper（被 shim 到主线程）与 Folia（真实区域线程）。</p>
 *
 * <p><b>调度器选择规则（务必遵守）：</b>
 * <ul>
 *   <li>修改实体状态（背包/gamemode/传送/音效）→ {@code runEntity*}（绑定玩家）</li>
 *   <li>读取某位置的世界/方块 → {@code runAtLocation}（绑定区域）</li>
 *   <li>全服周期任务（每 tick 扫玩家、actionbar 渲染）→ {@code runGlobalAtFixedRate}</li>
 *   <li>阻塞 IO（SQLite/JDBC）或 CPU 密集 → {@code runAsync*}（绝不在此修改实体/方块）</li>
 * </ul>
 */
public interface Scheduler {

    /** 在玩家所属线程上执行（用于修改该玩家状态）。retired 在玩家不在线/换世界时回调。 */
    void runEntity(Player player, Consumer<Player> action, Runnable retired);

    void runEntityDelayed(Player player, Consumer<Player> action, Runnable retired, long delayTicks);

    /** 按固定 tick 周期在玩家线程执行（用于 repeat 方块等绑定玩家的周期任务）。 */
    TaskHandle runEntityAtFixedRate(Player player, Consumer<Player> action, Runnable retired,
                                    long initialDelayTicks, long periodTicks);

    /** 在指定位置所属的区域线程执行（用于读取该位置的方块/世界状态）。 */
    void runAtLocation(Location location, Runnable action);

    /** 在全局区域线程执行。 */
    void runGlobal(Runnable action);

    void runGlobalDelayed(Runnable action, long delayTicks);

    /** 全局周期任务（每 tick 扫描所有玩家、actionbar 渲染等）。 */
    TaskHandle runGlobalAtFixedRate(Consumer<TaskHandle> action, long initialDelayTicks, long periodTicks);

    /** 在异步线程执行阻塞 IO（数据库）。禁止在此修改实体/方块。 */
    void runAsync(Runnable action);

    void runAsyncDelayed(Runnable action, long delay, TimeUnit unit);

    TaskHandle runAsyncAtFixedRate(Consumer<TaskHandle> action, long initialDelay, long period, TimeUnit unit);
}
