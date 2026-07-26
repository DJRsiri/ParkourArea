package dev.aaf.parkourArea.event;

import dev.aaf.parkourArea.zone.Zone;

import java.util.UUID;

/**
 * 玩家所在的最具体区域发生变化（每 2tick 检测后由 RegionTracker 发布）。
 * from/to 为变化前后的最具体区域，任一可为 null（表示之前/之后不在任何区域内）。
 */
public final class PlayerZoneChangeEvent extends InternalEvent {

    private final UUID playerId;
    private final Zone from;
    private final Zone to;

    public PlayerZoneChangeEvent(UUID playerId, Zone from, Zone to) {
        this.playerId = playerId;
        this.from = from;
        this.to = to;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    public Zone from() {
        return from;
    }

    public Zone to() {
        return to;
    }
}
