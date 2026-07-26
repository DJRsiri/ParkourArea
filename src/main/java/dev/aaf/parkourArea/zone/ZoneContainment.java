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

    /**
     * 两个区域是否几何相交（用于同级区域不可相交的校验）。
     *
     * <p>按精确形状判定，且仅贴面/共棱/相切（零体积重叠）<b>不算</b>相交，
     * 允许同级区域紧贴设置。</p>
     */
    public static boolean intersects(Zone a, Zone b) {
        boolean aSphere = a.shape() == SelectionShape.SPHERE;
        boolean bSphere = b.shape() == SelectionShape.SPHERE;
        if (!aSphere && !bSphere) {
            return cuboidIntersectsCuboid(a, b);
        }
        if (aSphere && bSphere) {
            double dx = a.centerX() - b.centerX();
            double dy = a.centerY() - b.centerY();
            double dz = a.centerZ() - b.centerZ();
            double r = a.radius() + b.radius();
            return dx * dx + dy * dy + dz * dz < r * r;
        }
        Zone sphere = aSphere ? a : b;
        Zone box = aSphere ? b : a;
        return sphereIntersectsCuboid(sphere, box);
    }

    /**
     * cuboid 相交：把 [min,max] 方块区间视作连续体 [min, max+1)，用严格不等式判定。
     * 贴面时 aMax+1 == bMin，重叠体积为零，不算相交。
     */
    private static boolean cuboidIntersectsCuboid(Zone a, Zone b) {
        return a.minX() < b.maxX() + 1 && a.maxX() + 1 > b.minX()
                && a.minY() < b.maxY() + 1 && a.maxY() + 1 > b.minY()
                && a.minZ() < b.maxZ() + 1 && a.maxZ() + 1 > b.minZ();
    }

    /** sphere 与 cuboid：圆心到 cuboid 连续体 [min, max+1) 的最近距离严格小于半径（相切不算）。 */
    private static boolean sphereIntersectsCuboid(Zone sphere, Zone box) {
        double cx = clamp(sphere.centerX(), box.minX(), box.maxX() + 1.0);
        double cy = clamp(sphere.centerY(), box.minY(), box.maxY() + 1.0);
        double cz = clamp(sphere.centerZ(), box.minZ(), box.maxZ() + 1.0);
        double dx = sphere.centerX() - cx;
        double dy = sphere.centerY() - cy;
        double dz = sphere.centerZ() - cz;
        return dx * dx + dy * dy + dz * dz < sphere.radius() * sphere.radius();
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
