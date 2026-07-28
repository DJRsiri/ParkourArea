package dev.aaf.parkourArea.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressStatusTest {

    @Test
    void visitNeverDegradesCompleted() {
        assertThat(ProgressStatus.COMPLETED.onVisit()).isEqualTo(ProgressStatus.COMPLETED);
    }

    @Test
    void visitPromotesNoneAndVisited() {
        assertThat(ProgressStatus.NONE.onVisit()).isEqualTo(ProgressStatus.VISITED);
        assertThat(ProgressStatus.VISITED.onVisit()).isEqualTo(ProgressStatus.VISITED);
    }

    @Test
    void parseToleratesGarbage() {
        assertThat(ProgressStatus.parse(null)).isEqualTo(ProgressStatus.NONE);
        assertThat(ProgressStatus.parse("xxx")).isEqualTo(ProgressStatus.NONE);
        assertThat(ProgressStatus.parse("COMPLETED")).isEqualTo(ProgressStatus.COMPLETED);
    }
}
