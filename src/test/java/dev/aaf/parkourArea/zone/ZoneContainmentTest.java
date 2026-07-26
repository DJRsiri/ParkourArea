package dev.aaf.parkourArea.zone;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneContainmentTest {

    private static final UUID W = UUID.randomUUID();

    @Test
    void cuboidContainsInteriorAndCornerPoints() {
        Zone z = Zone.cuboid(1, "t", ZoneType.GLOBAL, W, null, 0, 0, 0, 10, 10, 10);
        assertThat(z.containsPoint(5.5, 5.5, 5.5)).isTrue();
        assertThat(z.containsPoint(0.0, 0.0, 0.0)).isTrue();
        assertThat(z.containsPoint(10.9, 10.9, 10.9)).isTrue();
        assertThat(z.containsPoint(11.0, 5, 5)).isFalse();
        assertThat(z.containsPoint(5, -0.5, 5)).isFalse();
    }

    @Test
    void sphereContainsWithinRadius() {
        Zone z = Zone.sphere(1, "t", ZoneType.START, W, null, 0, 0, 0, 1.5);
        assertThat(z.containsPoint(0, 0, 0)).isTrue();
        assertThat(z.containsPoint(1.4, 0, 0)).isTrue();
        assertThat(z.containsPoint(1.6, 0, 0)).isFalse();
    }

    @Test
    void fullyContainedCuboidInCuboid() {
        Zone outer = Zone.cuboid(1, "o", ZoneType.GLOBAL, W, null, 0, 0, 0, 10, 10, 10);
        Zone inner = Zone.cuboid(2, "i", ZoneType.LOBBY, W, 1, 2, 2, 2, 8, 8, 8);
        assertThat(ZoneContainment.fullyContained(outer, inner)).isTrue();

        Zone outside = Zone.cuboid(3, "x", ZoneType.LOBBY, W, 1, 5, 5, 5, 15, 15, 15);
        assertThat(ZoneContainment.fullyContained(outer, outside)).isFalse();
    }

    @Test
    void intersectingSiblingsDetected() {
        Zone a = Zone.cuboid(1, "a", ZoneType.LEVEL, W, null, 0, 0, 0, 10, 10, 10);
        Zone b = Zone.cuboid(2, "b", ZoneType.LEVEL, W, null, 5, 5, 5, 15, 15, 15);
        Zone far = Zone.cuboid(3, "f", ZoneType.LEVEL, W, null, 100, 0, 0, 110, 10, 10);
        assertThat(ZoneContainment.intersects(a, b)).isTrue();
        assertThat(ZoneContainment.intersects(a, far)).isFalse();
    }
}
