package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.event.PlayerParkourTickEvent;
import dev.aaf.parkourArea.event.PlayerReachedCheckpointEvent;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.util.Locations;
import dev.aaf.parkourArea.util.SoundPlayer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * 中途存档点服务。每检测周期检查跑酷玩家脚下方块（y-0.1）是否为存档点方块。
 *
 * <p>踩到新存档点时：记录坐标、播放 {@code checkpoint-success-sound}（仅一次，开关可切）、发布事件。
 * actionbar 持续提示由 {@code ActionBarService} 在玩家仍站在存档点时渲染。</p>
 */
public final class CheckpointService {

    private final ParkourArea plugin;

    public CheckpointService(ParkourArea plugin) {
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
        Player player = Bukkit.getPlayer(e.playerId());
        if (player == null) {
            return;
        }
        Block below = Locations.blockBelowFeet(player);
        if (below.getType() != plugin.configService().settings().checkpointBlock()) {
            return;
        }
        int bx = below.getX();
        int by = below.getY();
        int bz = below.getZ();
        // 仅在新位置存档点时记录 + 播放音效（避免每 tick 重复）
        if (session.hasCheckpoint()
                && session.checkpointX() == bx
                && session.checkpointY() == by
                && session.checkpointZ() == bz) {
            return;
        }
        session.setCheckpoint(bx, by, bz);
        if (session.shouldPlayCheckpointSound()) {
            String sound = plugin.configService().settings().checkpointSuccessSound();
            plugin.scheduler().runEntity(player, p -> SoundPlayer.play(p, sound), () -> {});
        }
        plugin.eventBus().publish(new PlayerReachedCheckpointEvent(e.playerId(), bx, by, bz));
    }
}
