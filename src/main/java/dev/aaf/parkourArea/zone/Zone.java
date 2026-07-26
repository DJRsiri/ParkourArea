package dev.aaf.parkourArea.zone;

import java.util.UUID;

/**
 * 跑酷区域实体。不可变（除 name 外）。
 *
 * <p>CUBOID 存两个角的方块坐标 min/max；SPHERE 存中心点与半径。
 * 世界用 UUID（重命名安全）。</p>
 */
public final class Zone {

    private final int id;
    private String name;
    private final ZoneType type;
    private final SelectionShape shape;
    private final UUID worldUid;
    private final Integer parentId;

    // CUBOID
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    // SPHERE
    private final double centerX, centerY, centerZ, radius;
    // 传送点（可空，部分字段可空；非 final，可通过 edit 命令/zones.yml 修改）
    private ZoneSpawn spawn;

    private Zone(int id, String name, ZoneType type, SelectionShape shape, UUID worldUid,
                 Integer parentId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                 double centerX, double centerY, double centerZ, double radius, ZoneSpawn spawn) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.type = type;
        this.shape = shape;
        this.worldUid = worldUid;
        this.parentId = parentId;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.centerX = centerX; this.centerY = centerY; this.centerZ = centerZ;
        this.radius = radius;
        this.spawn = spawn == null || spawn.isEmpty() ? null : spawn;
    }

    public static Zone cuboid(int id, String name, ZoneType type, UUID worldUid, Integer parentId,
                              int x1, int y1, int z1, int x2, int y2, int z2) {
        return cuboid(id, name, type, worldUid, parentId, x1, y1, z1, x2, y2, z2, null);
    }

    public static Zone cuboid(int id, String name, ZoneType type, UUID worldUid, Integer parentId,
                              int x1, int y1, int z1, int x2, int y2, int z2, ZoneSpawn spawn) {
        return new Zone(id, name, type, SelectionShape.CUBOID, worldUid, parentId,
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2),
                0, 0, 0, 0, spawn);
    }

    public static Zone sphere(int id, String name, ZoneType type, UUID worldUid, Integer parentId,
                              double cx, double cy, double cz, double radius) {
        return sphere(id, name, type, worldUid, parentId, cx, cy, cz, radius, null);
    }

    public static Zone sphere(int id, String name, ZoneType type, UUID worldUid, Integer parentId,
                              double cx, double cy, double cz, double radius, ZoneSpawn spawn) {
        return new Zone(id, name, type, SelectionShape.SPHERE, worldUid, parentId,
                0, 0, 0, 0, 0, 0, cx, cy, cz, radius, spawn);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName == null ? "" : newName;
    }

    public ZoneType type() {
        return type;
    }

    public SelectionShape shape() {
        return shape;
    }

    public UUID worldUid() {
        return worldUid;
    }

    public Integer parentId() {
        return parentId;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    public double centerZ() {
        return centerZ;
    }

    public double radius() {
        return radius;
    }

    public ZoneSpawn spawn() {
        return spawn;
    }

    public void setSpawn(ZoneSpawn spawn) {
        this.spawn = spawn == null || spawn.isEmpty() ? null : spawn;
    }

    /**
     * 玩家位置是否在区域内。
     *
     * <p>CUBOID：用玩家所在方块（floor 后的方块坐标）是否落在 [min,max] 区间。
     * SPHERE：用玩家精确位置与中心点的距离平方是否 <= r^2。</p>
     */
    public boolean containsPoint(double px, double py, double pz) {
        if (shape == SelectionShape.CUBOID) {
            int bx = floor(px);
            int by = floor(py);
            int bz = floor(pz);
            return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
        }
        double dx = px - centerX;
        double dy = py - centerY;
        double dz = pz - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }

    /** 按 id 判等（reload 后同 id 视为同一区域，避免误触发区域变更事件）。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Zone other)) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
