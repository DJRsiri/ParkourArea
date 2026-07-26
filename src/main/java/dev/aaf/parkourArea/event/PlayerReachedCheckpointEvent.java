package dev.aaf.parkourArea.event;

import java.util.UUID;

/** 玩家踩到存档点方块，中途存档点已记录。 */
public final class PlayerReachedCheckpointEvent extends InternalEvent {

    private final UUID playerId;
    private final int x;
    private final int y;
    private final int z;

    public PlayerReachedCheckpointEvent(UUID playerId, int x, int y, int z) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }
}
