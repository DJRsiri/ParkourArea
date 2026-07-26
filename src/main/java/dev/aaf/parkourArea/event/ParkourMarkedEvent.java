package dev.aaf.parkourArea.event;

import java.util.UUID;

/** 玩家进入全局区域且满足标记条件，被标记为跑酷玩家（已快照状态、切换游戏模式、替换工具栏）。 */
public final class ParkourMarkedEvent extends InternalEvent {

    private final UUID playerId;

    public ParkourMarkedEvent(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }
}
