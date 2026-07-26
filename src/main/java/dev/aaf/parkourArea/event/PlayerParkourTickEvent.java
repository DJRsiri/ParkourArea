package dev.aaf.parkourArea.event;

import dev.aaf.parkourArea.player.ParkourPlayer;

import java.util.UUID;

/**
 * 每 detect-interval 对每个跑酷玩家发布一次（global region 线程）。
 * 供 CheckpointService / AntiIdleService / BlockCommandService 等订阅做周期检测。
 */
public final class PlayerParkourTickEvent extends InternalEvent {

    private final UUID playerId;
    private final ParkourPlayer session;

    public PlayerParkourTickEvent(UUID playerId, ParkourPlayer session) {
        this.playerId = playerId;
        this.session = session;
    }

    @Override
    public UUID playerId() {
        return playerId;
    }

    public ParkourPlayer session() {
        return session;
    }
}
