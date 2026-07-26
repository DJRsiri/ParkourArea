package dev.aaf.parkourArea.event;

import java.util.UUID;

/** 玩家离开全局区域（或断线/换世界），取消跑酷玩家标记并恢复其原有状态。 */
public final class ParkourUnmarkedEvent extends InternalEvent {

    private final UUID playerId;

    public ParkourUnmarkedEvent(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }
}
