package dev.aaf.parkourArea.hooks.worldedit;

import java.util.UUID;

/** WorldEdit 选区信息（cuboid 的两个角 + 世界）。 */
public record SelectionInfo(UUID worldUid, int x1, int y1, int z1, int x2, int y2, int z2) {
}
