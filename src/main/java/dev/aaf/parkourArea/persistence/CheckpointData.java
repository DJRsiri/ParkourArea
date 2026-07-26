package dev.aaf.parkourArea.persistence;

/** 玩家在某关的存档点数据（mid=踩金块中途存档点；last=最后记录的存档点，用于「回到最后存档点」）。 */
public record CheckpointData(
        boolean hasMid, int midX, int midY, int midZ,
        boolean hasLast, int lastX, int lastY, int lastZ
) {
    public static CheckpointData empty() {
        return new CheckpointData(false, 0, 0, 0, false, 0, 0, 0);
    }
}
