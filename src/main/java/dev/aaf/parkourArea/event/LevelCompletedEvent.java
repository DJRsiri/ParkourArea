package dev.aaf.parkourArea.event;

import dev.aaf.parkourArea.zone.Zone;

import java.util.UUID;

/** 玩家完成某关（已判定非跳关、phase=RUNNING 到达终点）。 */
public final class LevelCompletedEvent extends InternalEvent {

    private final UUID playerId;
    private final Zone level;
    private final long durationMillis;

    public LevelCompletedEvent(UUID playerId, Zone level, long durationMillis) {
        this.playerId = playerId;
        this.level = level;
        this.durationMillis = durationMillis;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    public Zone level() {
        return level;
    }

    public long durationMillis() {
        return durationMillis;
    }
}
