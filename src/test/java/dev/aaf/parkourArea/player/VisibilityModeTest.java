package dev.aaf.parkourArea.player;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisibilityModeTest {

    @Test
    void nextCyclesThroughAllModes() {
        assertThat(VisibilityMode.FULL.next()).isEqualTo(VisibilityMode.HALF);
        assertThat(VisibilityMode.HALF.next()).isEqualTo(VisibilityMode.TENTH);
        assertThat(VisibilityMode.TENTH.next()).isEqualTo(VisibilityMode.HIDDEN);
        assertThat(VisibilityMode.HIDDEN.next()).isEqualTo(VisibilityMode.FULL);
    }

    @Test
    void parseReturnsFallbackOnInvalid() {
        assertThat(VisibilityMode.parse("half", VisibilityMode.FULL)).isEqualTo(VisibilityMode.HALF);
        assertThat(VisibilityMode.parse("HALF", VisibilityMode.FULL)).isEqualTo(VisibilityMode.HALF);
        assertThat(VisibilityMode.parse(null, VisibilityMode.FULL)).isEqualTo(VisibilityMode.FULL);
        assertThat(VisibilityMode.parse("garbage", VisibilityMode.TENTH)).isEqualTo(VisibilityMode.TENTH);
    }
}
