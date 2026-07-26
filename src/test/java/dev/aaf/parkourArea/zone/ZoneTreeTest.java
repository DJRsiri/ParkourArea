package dev.aaf.parkourArea.zone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTreeTest {

    private static final UUID W = UUID.randomUUID();

    @Test
    void findMostSpecificReturnsHighestPriorityZone() {
        ZoneTree tree = new ZoneTree();
        Zone g = Zone.cuboid(1, "g", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100);
        Zone lobby = Zone.cuboid(2, "l", ZoneType.LOBBY, W, 1, 10, 10, 10, 50, 50, 50);
        Zone level = Zone.cuboid(3, "lv", ZoneType.LEVEL, W, 2, 15, 15, 15, 20, 20, 20);
        tree.add(g);
        tree.add(lobby);
        tree.add(level);

        assertThat(tree.findMostSpecific(W, 16, 16, 16)).isEqualTo(level);
        assertThat(tree.findMostSpecific(W, 12, 12, 12)).isEqualTo(lobby);
        assertThat(tree.findMostSpecific(W, 5, 5, 5)).isEqualTo(g);
        assertThat(tree.findMostSpecific(W, 200, 200, 200)).isNull();
    }

    @Test
    void findChainFromMostSpecificToGlobal() {
        ZoneTree tree = new ZoneTree();
        Zone g = Zone.cuboid(1, "g", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100);
        Zone level = Zone.cuboid(3, "lv", ZoneType.LEVEL, W, 1, 15, 15, 15, 20, 20, 20);
        tree.add(g);
        tree.add(level);

        List<Zone> chain = tree.findChain(W, 16, 16, 16);
        assertThat(chain).extracting(Zone::id).containsExactly(3, 1);
    }

    @Test
    void descendentsIncludeSelfAndAllChildren() {
        ZoneTree tree = new ZoneTree();
        Zone g = Zone.cuboid(1, "g", ZoneType.GLOBAL, W, null, 0, 0, 0, 100, 100, 100);
        Zone lobby = Zone.cuboid(2, "l", ZoneType.LOBBY, W, 1, 10, 10, 10, 50, 50, 50);
        Zone level = Zone.cuboid(3, "lv", ZoneType.LEVEL, W, 2, 15, 15, 15, 20, 20, 20);
        tree.add(g);
        tree.add(lobby);
        tree.add(level);

        List<Zone> desc = tree.descendentsOf(1);
        assertThat(desc).extracting(Zone::id).containsExactlyInAnyOrder(1, 2, 3);

        assertThat(tree.descendentsOf(2)).extracting(Zone::id).containsExactlyInAnyOrder(2, 3);
    }
}
