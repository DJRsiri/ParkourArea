package dev.aaf.parkourArea.zone;

/** 区域几何包含判定工具（cuboid/sphere）。 */
public final class ZoneContainment {

    private ZoneContainment() {}

    /**
     * 判断 {@code inner} 是否完全几何包含于 {@code outer}（用于父子层级校验）。
     *
     * <p>CUBOID inner：min/max 四角必须在 outer 内。
     * SPHERE inner：取其外接 AABB（center±radius）的 8 个角点都必须在 outer 内。</p>
     */
    public static boolean fullyContained(Zone outer, Zone inner) {
        if (inner.shape() == SelectionShape.CUBOID) {
            // 检查 8 个角
            return outer.containsPoint(inner.minX(), inner.minY(), inner.minZ())
                    && outer.containsPoint(inner.maxX(), inner.minY(), inner.minZ())
                    && outer.containsPoint(inner.minX(), inner.maxY(), inner.minZ())
                    && outer.containsPoint(inner.maxX(), inner.maxY(), inner.minZ())
                    && outer.containsPoint(inner.minX(), inner.minY(), inner.maxZ())
                    && outer.containsPoint(inner.maxX(), inner.minY(), inner.maxZ())
                    && outer.containsPoint(inner.minX(), inner.maxY(), inner.maxZ())
                    && outer.containsPoint(inner.maxX(), inner.maxY(), inner.maxZ());
        }
        // SPHERE inner：检查外接 AABB 8 角
        double r = inner.radius();
        double cx = inner.centerX(), cy = inner.centerY(), cz = inner.centerZ();
        double[][] corners = {
                {cx - r, cy - r, cz - r}, {cx + r, cy - r, cz - r},
                {cx - r, cy + r, cz - r}, {cx + r, cy + r, cz - r},
                {cx - r, cy - r, cz + r}, {cx + r, cy - r, cz + r},
                {cx - r, cy + r, cz + r}, {cx + r, cy + r, cz + r}
        };
        for (double[] c : corners) {
            if (!outer.containsPoint(c[0], c[1], c[2])) {
                return false;
            }
        }
        return true;
    }

    /** 两个区域是否几何相交（用于同级区域不可相交的校验）。 */
    public static boolean intersects(Zone a, Zone b) {
        // AABB 相交快速排除
        if (!aabbIntersects(a, b)) {
            return false;
        }
        // 简化：AABB 相交即视为可能相交（保守判定，宁可误拒也不放过）
        // 对跑酷场景足够精确（区域通常是长方体/球体，重叠判定用 AABB 已覆盖大部分）
        return true;
    }

    private static boolean aabbIntersects(Zone a, Zone b) {
        double[] aMin = aabbMin(a);
        double[] aMax = aabbMax(a);
        double[] bMin = aabbMin(b);
        double[] bMax = aabbMax(b);
        return aMin[0] <= bMax[0] && aMax[0] >= bMin[0]
                && aMin[1] <= bMax[1] && aMax[1] >= bMin[1]
                && aMin[2] <= bMax[2] && aMax[2] >= bMin[2];
    }

    private static double[] aabbMin(Zone z) {
        return z.shape() == SelectionShape.CUBOID
                ? new double[]{z.minX(), z.minY(), z.minZ()}
                : new double[]{z.centerX() - z.radius(), z.centerY() - z.radius(), z.centerZ() - z.radius()};
    }

    private static double[] aabbMax(Zone z) {
        return z.shape() == SelectionShape.CUBOID
                ? new double[]{z.maxX() + 1, z.maxY() + 1, z.maxZ() + 1}
                : new double[]{z.centerX() + z.radius(), z.centerY() + z.radius(), z.centerZ() + z.radius()};
    }
}
