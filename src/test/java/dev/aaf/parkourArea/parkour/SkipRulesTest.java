package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.player.PlayerPhase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkipRulesTest {

    // ---- startIsSkip：顺序维度 ----

    @Test
    void startSkipOnlyWhenLevelBeyondNextExpected() {
        assertThat(SkipRules.startIsSkip(true, false, 5, 3)).isTrue();   // 5 > 3 跳关
        assertThat(SkipRules.startIsSkip(true, false, 3, 3)).isFalse();  // 本关即下一关
        assertThat(SkipRules.startIsSkip(true, false, 1, 3)).isFalse();  // 回走低关
        assertThat(SkipRules.startIsSkip(true, false, 5, -1)).isFalse(); // 全通关
    }

    @Test
    void startSkipRespectsSwitches() {
        assertThat(SkipRules.startIsSkip(false, false, 5, 3)).isFalse(); // 检测关闭
        assertThat(SkipRules.startIsSkip(true, true, 5, 3)).isFalse();   // 自由选关
    }

    // ---- startIgnored：RUNNING 途经/回走他关起点 ----

    @Test
    void startIgnoredWhenRunningOtherLevel() {
        assertThat(SkipRules.startIgnored(PlayerPhase.RUNNING, 2, 1)).isTrue();  // 回走第 1 关
        assertThat(SkipRules.startIgnored(PlayerPhase.RUNNING, 2, 3)).isTrue();  // 途经第 3 关
        assertThat(SkipRules.startIgnored(PlayerPhase.RUNNING, 2, 2)).isFalse(); // 回到本关起点
        assertThat(SkipRules.startIgnored(PlayerPhase.AT_START, 2, 1)).isFalse(); // 非 RUNNING 不豁免
        assertThat(SkipRules.startIgnored(PlayerPhase.RUNNING, null, 1)).isFalse();
    }

    // ---- endIgnored：途经/未选关的终点 ----

    @Test
    void endIgnoredUnlessSelectedLevel() {
        assertThat(SkipRules.endIgnored(null, 1)).isTrue();   // 闲逛踩终点
        assertThat(SkipRules.endIgnored(2, 1)).isTrue();      // 途经他关终点
        assertThat(SkipRules.endIgnored(1, 1)).isFalse();     // 所选关终点
    }

    // ---- endIsSkip：未从起点出发到达所选关终点 ----

    @Test
    void endSkipOnlyWhenNotDeparted() {
        assertThat(SkipRules.endIsSkip(true, PlayerPhase.AT_START)).isTrue();
        assertThat(SkipRules.endIsSkip(true, PlayerPhase.LEVEL_SELECTED)).isTrue();
        assertThat(SkipRules.endIsSkip(true, PlayerPhase.RUNNING)).isFalse();
        assertThat(SkipRules.endIsSkip(true, PlayerPhase.COMPLETED)).isFalse();   // 通关后停留
        assertThat(SkipRules.endIsSkip(true, PlayerPhase.INVALIDATED)).isFalse();
        assertThat(SkipRules.endIsSkip(false, PlayerPhase.AT_START)).isFalse();   // 检测关闭
    }
}
