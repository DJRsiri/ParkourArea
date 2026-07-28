package dev.aaf.parkourArea.config;

import dev.aaf.parkourArea.player.VisibilityMode;
import org.bukkit.GameMode;
import org.bukkit.Material;

import java.util.Set;

/** config.yml 的强类型映射（不可变）。 */
public record Settings(
        int detectIntervalTicks,
        int actionbarIntervalTicks,
        int antiIdleSeconds,
        double antiIdleRotationThresholdDeg,
        int topRecordCount,
        Set<GameMode> requireGameMode,
        Material checkpointBlock,
        String checkpointSuccessSound,
        boolean checkpointSoundEnabled,
        long ratingFlickerIntervalMillis,
        boolean editModeDefault,
        boolean debug,
        boolean soundEnabled,
        boolean blockSoundEnabled,
        VisibilityMode defaultVisibilityMode,
        boolean teleportKeepRotation,
        boolean skipDetection,
        boolean allowAnySelectable
) {
    /** 玩家当前游戏模式是否满足标记为跑酷玩家的要求。 */
    public boolean requiresGameMode(GameMode mode) {
        return requireGameMode.contains(mode);
    }
}
