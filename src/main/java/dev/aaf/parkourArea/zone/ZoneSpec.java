package dev.aaf.parkourArea.zone;

import java.util.UUID;

/** 创建区域时的参数值对象（坐标参数，不含 id）。 */
public record ZoneSpec(
        ZoneType type, String name, UUID worldUid, Integer parentId,
        SelectionShape shape,
        int x1, int y1, int z1, int x2, int y2, int z2,
        double cx, double cy, double cz, double radius
) {

    public static ZoneSpec cuboid(ZoneType type, String name, UUID world, Integer parentId,
                                  int x1, int y1, int z1, int x2, int y2, int z2) {
        return new ZoneSpec(type, name, world, parentId, SelectionShape.CUBOID,
                x1, y1, z1, x2, y2, z2, 0, 0, 0, 0);
    }

    public static ZoneSpec sphere(ZoneType type, String name, UUID world, Integer parentId,
                                  double cx, double cy, double cz, double radius) {
        return new ZoneSpec(type, name, world, parentId, SelectionShape.SPHERE,
                0, 0, 0, 0, 0, 0, cx, cy, cz, radius);
    }

    /** 转为带指定 id 的 Zone。 */
    public Zone toZone(int id) {
        if (shape == SelectionShape.CUBOID) {
            return Zone.cuboid(id, name, type, worldUid, parentId, x1, y1, z1, x2, y2, z2);
        }
        return Zone.sphere(id, name, type, worldUid, parentId, cx, cy, cz, radius);
    }

    /** 临时 Zone（id=-1）用于层级校验。 */
    public Zone toTempZone() {
        return toZone(-1);
    }
}
