package dev.aaf.parkourArea.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ParkourPlayerSoundTest {

    @Test
    void masterSwitchGatesSubitems() {
        ParkourPlayer p = new ParkourPlayer(UUID.randomUUID());
        // 默认总开关与子项全开
        assertThat(p.shouldPlayCheckpointSound()).isTrue();
        assertThat(p.shouldPlayBlockSound()).isTrue();

        // 关闭总开关 → 两个子项都被闭锁
        p.soundEnabled(false);
        assertThat(p.shouldPlayCheckpointSound()).isFalse();
        assertThat(p.shouldPlayBlockSound()).isFalse();

        // 开启总开关，单独关闭存档点子项
        p.soundEnabled(true);
        p.checkpointSoundEnabled(false);
        assertThat(p.shouldPlayCheckpointSound()).isFalse();
        assertThat(p.shouldPlayBlockSound()).isTrue();

        // 单独关闭效果块子项
        p.blockSoundEnabled(false);
        assertThat(p.shouldPlayBlockSound()).isFalse();
    }
}
