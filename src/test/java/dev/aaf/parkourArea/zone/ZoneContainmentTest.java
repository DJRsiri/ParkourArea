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

    @Test
    void faceTouchingCuboidsDoNotIntersect() {
        // 实机反馈场景：x 方向紧贴的两个 LEVEL（-45 与 -46 相邻），应允许创建
        Zone a = Zone.cuboid(1, "a", ZoneType.LEVEL, W, null, -45, 66, -61, -38, 71, -55);
        Zone b = Zone.cuboid(2, "b", ZoneType.LEVEL, W, null, -53, 66, -61, -46, 71, -55);
        assertThat(ZoneContainment.intersects(a, b)).isFalse();
        assertThat(ZoneContainment.intersects(b, a)).isFalse();
        // 仅重叠 1 格才算相交
        Zone c = Zone.cuboid(3, "c", ZoneType.LEVEL, W, null, -45, 66, -61, -40, 71, -55);
        assertThat(ZoneContainment.intersects(a, c)).isTrue();
    }

    @Test
    void edgeAndCornerTouchingCuboidsDoNotIntersect() {
        Zone a = Zone.cuboid(1, "a", ZoneType.LEVEL, W, null, 0, 0, 0, 10, 10, 10);
        // y 贴面
        Zone above = Zone.cuboid(2, "b", ZoneType.LEVEL, W, null, 0, 11, 0, 10, 20, 10);
        // 棱相贴（x、z 同时贴面）
        Zone edge = Zone.cuboid(3, "c", ZoneType.LEVEL, W, null, 11, 0, 11, 20, 10, 20);
        // 角相贴（x、y、z 三向贴面）
        Zone corner = Zone.cuboid(4, "d", ZoneType.LEVEL, W, null, 11, 11, 11, 20, 20, 20);
        assertThat(ZoneContainment.intersects(a, above)).isFalse();
        assertThat(ZoneContainment.intersects(a, edge)).isFalse();
        assertThat(ZoneContainment.intersects(a, corner)).isFalse();
    }

    @Test
    void tangentSpheresDoNotIntersect() {
        Zone a = Zone.sphere(1, "a", ZoneType.START, W, null, 0, 0, 0, 2.0);
        Zone b = Zone.sphere(2, "b", ZoneType.END, W, null, 4, 0, 0, 2.0);   // 相切 dist=4=r+r
        Zone c = Zone.sphere(3, "c", ZoneType.END, W, null, 3.9, 0, 0, 2.0); // 相交
        assertThat(ZoneContainment.intersects(a, b)).isFalse();
        assertThat(ZoneContainment.intersects(a, c)).isTrue();
    }

    @Test
    void sphereCuboidTouchingDoesNotIntersect() {
        Zone box = Zone.cuboid(1, "a", ZoneType.LEVEL, W, null, 0, 0, 0, 10, 10, 10);
        // 球心贴 cuboid 面外 1 格、半径 1 → 与连续体 [0,11) 相切
        Zone tangent = Zone.sphere(2, "b", ZoneType.START, W, null, -1.0, 5, 5, 1.0);
        // 半径 1.5 → 侵入 0.5
        Zone overlap = Zone.sphere(3, "c", ZoneType.START, W, null, -1.0, 5, 5, 1.5);
        // 球心在 cuboid 内
        Zone inside = Zone.sphere(4, "d", ZoneType.START, W, null, 5, 5, 5, 0.5);
        assertThat(ZoneContainment.intersects(box, tangent)).isFalse();
        assertThat(ZoneContainment.intersects(box, overlap)).isTrue();
        assertThat(ZoneContainment.intersects(box, inside)).isTrue();
        // 对称方向（sphere 在前）
        assertThat(ZoneContainment.intersects(tangent, box)).isFalse();
    }
}
