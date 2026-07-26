package dev.aaf.parkourArea.actionbar;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.parkour.RatingService;
import dev.aaf.parkourArea.parkour.TimerService;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.util.ColorUtil;
import dev.aaf.parkourArea.util.Locations;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * actionbar 渲染服务。每 tick（actionbar-interval-ticks，默认 1）在 global region 线程渲染所有跑酷玩家。
 *
 * <p>格式（游玩中）：当前用时 + 星级 / 最佳用时。
 * 踩在存档点方块上时持续显示存档点提示（替代计时）。</p>
 */
public final class ActionBarService {

    private final ParkourArea plugin;
    private final RatingService ratingService;
    private final TimerService timerService;
    private dev.aaf.parkourArea.concurrency.TaskHandle task;

    public ActionBarService(ParkourArea plugin, RatingService ratingService, TimerService timerService) {
        this.plugin = plugin;
        this.ratingService = ratingService;
        this.timerService = timerService;
    }

    public void start() {
        int interval = plugin.configService().settings().actionbarIntervalTicks();
        task = plugin.scheduler().runGlobalAtFixedRate(this::tick, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick(dev.aaf.parkourArea.concurrency.TaskHandle handle) {
        long now = System.currentTimeMillis();
        for (ParkourPlayer session : plugin.sessionService().all().values()) {
            Player player = Bukkit.getPlayer(session.uuid());
            if (player == null) {
                continue;
            }
            Component msg = render(player, session, now);
            if (msg != null) {
                player.sendActionBar(msg);
            }
        }
    }

    private Component render(Player player, ParkourPlayer session, long now) {
        PlayerPhase phase = session.phase();
        if (phase == PlayerPhase.AT_START) {
            return plugin.messages().plain("actionbar.at-start");
        }
        if (phase == PlayerPhase.RUNNING || phase == PlayerPhase.AT_CHECKPOINT) {
            return renderPlaying(player, session, now);
        }
        if (phase == PlayerPhase.COMPLETED) {
            return plugin.messages().plain("actionbar.completed",
                    Map.of("time", formatDuration(session.completedDuration())));
        }
        return null;
    }

    private Component renderPlaying(Player player, ParkourPlayer session, long now) {
        if (session.selectedLevel() == null) {
            return null;
        }
        int levelId = session.selectedLevel().id();
        // 踩在存档点方块上 → 显示存档点提示（直到离开）
        Block below = Locations.blockBelowFeet(player);
        boolean onCheckpoint = session.hasCheckpoint()
                && below.getType() == plugin.configService().settings().checkpointBlock();
        if (onCheckpoint) {
            return plugin.messages().plain("actionbar.checkpoint");
        }
        long current = now - session.levelStartedAt();
        Long best = timerService.getBest(session.uuid(), levelId);
        String stars = ratingService.renderStars(levelId, current, now);
        // stars 已是 legacy 码，拼接到模板后统一解析颜色
        String merged = plugin.messages().raw("actionbar.playing")
                .replace("{current}", formatDuration(current))
                .replace("{best}", best == null ? "--" : formatDuration(best))
                .replace("{stars}", stars);
        return ColorUtil.parse(merged);
    }

    /** 格式化为 m:ss.SSS。 */
    public static String formatDuration(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long totalSec = millis / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        long ms = millis % 1000;
        return String.format("%d:%02d.%03d", m, s, ms);
    }
}
