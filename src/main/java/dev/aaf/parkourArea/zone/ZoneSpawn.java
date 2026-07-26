package dev.aaf.parkourArea.zone;

/**
 * 区域传送点配置。所有字段均可空——表示仅手动指定部分维度，其余走默认。
 *
 * <p>语义：
 * <ul>
 *   <li>挂在 {@link ZoneType#LOBBY} 区域：大厅传送点（坐标 x/y/z + 朝向 yaw/pitch）</li>
 *   <li>挂在 {@link ZoneType#LEVEL} 区域：该关起点传送的朝向（坐标取自所属 START 区域）</li>
 *   <li>挂在 {@link ZoneType#START} 区域：起点传送坐标（可选，缺省取 START 中心）</li>
 * </ul>
 * 未指定的字段：坐标=区域中心 + 该列最高非空气方块上一格；朝向=0/0。</p>
 */
public record ZoneSpawn(Double x, Double y, Double z, Double yaw, Double pitch) {

    public static ZoneSpawn of(Double x, Double y, Double z, Double yaw, Double pitch) {
        return new ZoneSpawn(x, y, z, yaw, pitch);
    }

    public boolean isEmpty() {
        return x == null && y == null && z == null && yaw == null && pitch == null;
    }

    /** 用新值覆盖非 null 字段，null 的沿用本对象对应字段（用于部分更新，如只改 yaw）。 */
    public ZoneSpawn merge(Double nx, Double ny, Double nz, Double nyaw, Double npitch) {
        return new ZoneSpawn(
                nx != null ? nx : x,
                ny != null ? ny : y,
                nz != null ? nz : z,
                nyaw != null ? nyaw : yaw,
                npitch != null ? npitch : pitch);
    }
}
