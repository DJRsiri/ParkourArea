package dev.aaf.parkourArea.zone;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneHierarchyTest {

    private static final UUID W = UUID.randomUUID();

    @Test
    void globalWithoutParentIsAllowed() {
        ZoneTree tree = new ZoneTree();
        Zone g = Zone.cuboid(1, "g", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100);
        assertThat(ZoneHierarchy.canCreate(g, null, tree).valid()).isTrue();
    }

    @Test
    void secondGlobalNonIntersectingAllowed() {
        ZoneTree tree = new ZoneTree();
        tree.add(Zone.cuboid(1, "g1", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100));
        Zone g2 = Zone.cuboid(2, "g2", ZoneType.GLOBAL, W, null, 200, 0, 0, 300, 100, 100);
        assertThat(ZoneHierarchy.canCreate(g2, null, tree).valid()).isTrue();
    }

    @Test
    void secondGlobalIntersectingRejected() {
        ZoneTree tree = new ZoneTree();
        tree.add(Zone.cuboid(1, "g1", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100));
        Zone g2 = Zone.cuboid(2, "g2", ZoneType.GLOBAL, W, null, 50, 0, 0, 150, 100, 100); // 与 g1 相交
        assertThat(ZoneHierarchy.canCreate(g2, null, tree).valid()).isFalse();
    }

    @Test
    void globalInOtherWorldAllowed() {
        ZoneTree tree = new ZoneTree();
        tree.add(Zone.cuboid(1, "g1", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100));
        UUID w2 = UUID.randomUUID();
        Zone g2 = Zone.cuboid(2, "g2", ZoneType.GLOBAL, w2, null, 0, 0, 0, 100, 100, 100);
        assertThat(ZoneHierarchy.canCreate(g2, null, tree).valid()).isTrue();
    }

    @Test
    void nonGlobalWithoutParentRejected() {
        ZoneTree tree = new ZoneTree();
        Zone lobby = Zone.cuboid(1, "l", ZoneType.LOBBY, W, null, 0, 0, 0, 50, 50, 50);
        assertThat(ZoneHierarchy.canCreate(lobby, null, tree).valid()).isFalse();
    }

    @Test
    void levelUnderLobbyAllowed() {
        ZoneTree tree = new ZoneTree();
        Zone g = Zone.cuboid(1, "g", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100);
        tree.add(g);
        Zone lobby = Zone.cuboid(2, "l", ZoneType.LOBBY, W, 1, 10, 10, 10, 50, 50, 50);
        tree.add(lobby);
        Zone level = Zone.cuboid(3, "lv", ZoneType.LEVEL, W, 2, 15, 15, 15, 20, 20, 20);
        assertThat(ZoneHierarchy.canCreate(level, lobby, tree).valid()).isTrue();
    }

    @Test
    void startCannotBeDirectlyUnderLobby() {
        ZoneTree tree = new ZoneTree();
        Zone lobby = Zone.cuboid(2, "l", ZoneType.LOBBY, W, 1, 10, 10, 10, 50, 50, 50);
        Zone start = Zone.cuboid(3, "s", ZoneType.START, W, 2, 20, 20, 20, 21, 21, 21);
        assertThat(ZoneHierarchy.canCreate(start, lobby, tree).valid()).isFalse();
    }

    @Test
    void childMustBeGeometricallyInsideParent() {
        ZoneTree tree = new ZoneTree();
        Zone lobby = Zone.cuboid(2, "l", ZoneType.LOBBY, W, 1, 10, 10, 10, 50, 50, 50);
        Zone levelOutside = Zone.cuboid(3, "lv", ZoneType.LEVEL, W, 2, 40, 40, 40, 80, 80, 80);
        assertThat(ZoneHierarchy.canCreate(levelOutside, lobby, tree).valid()).isFalse();
    }

    @Test
    void intersectingSiblingsRejected() {
        ZoneTree tree = new ZoneTree();
        Zone g = Zone.cuboid(1, "g", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100);
        tree.add(g);
        Zone l1 = Zone.cuboid(2, "l1", ZoneType.LEVEL, W, 1, 10, 10, 10, 30, 30, 30);
        tree.add(l1);
        Zone l2 = Zone.cuboid(3, "l2", ZoneType.LEVEL, W, 1, 20, 20, 20, 40, 40, 40); // 与 l1 相交
        assertThat(ZoneHierarchy.canCreate(l2, g, tree).valid()).isFalse();
    }
}
