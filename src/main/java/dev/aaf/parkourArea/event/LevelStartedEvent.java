package dev.aaf.parkourArea.event;

import dev.aaf.parkourArea.zone.Zone;

import java.util.UUID;

/** 玩家离开起点正式开始某关计时（AT_START → RUNNING）。 */
public final class LevelStartedEvent extends InternalEvent {

    private final UUID playerId;
    private final Zone level;
    private final long startedAtMillis;

    public LevelStartedEvent(UUID playerId, Zone level, long startedAtMillis) {
        this.playerId = playerId;
        this.level = level;
        this.startedAtMillis = startedAtMillis;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    public Zone level() {
        return level;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }
}
