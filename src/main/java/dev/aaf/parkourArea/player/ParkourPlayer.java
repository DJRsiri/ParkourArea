package dev.aaf.parkourArea.player;

import dev.aaf.parkourArea.zone.Zone;

import java.util.UUID;

/**
 * 跑酷玩家会话（不是 Bukkit Player）。
 *
 * <p>仅在 global region 线程修改字段（由每 tick 检测任务与会话协调器驱动）。</p>
 */
public final class ParkourPlayer {

    private final UUID uuid;

    private PlayerPhase phase = PlayerPhase.OUTSIDE;
    private Zone currentZone;
    private Zone selectedLevel;
    private long levelStartedAt;
    private PlayerState savedState;

    // 中途存档点（运行中最后记录的存档点，用于「回到最后存档点」）
    private boolean hasCheckpoint;
    private int checkpointX;
    private int checkpointY;
    private int checkpointZ;

    // 防挂机：最后位置/视角与时间戳
    private long lastMoveAt;
    private double lastX;
    private double lastY;
    private double lastZ;
    private float lastYaw;
    private float lastPitch;

    private boolean checkpointSoundEnabled = true;
    private boolean soundEnabled = true;
    private boolean blockSoundEnabled = true;
    private VisibilityMode visibilityMode = VisibilityMode.FULL;

    public ParkourPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public PlayerPhase phase() {
        return phase;
    }

    public void phase(PlayerPhase phase) {
        this.phase = phase;
    }

    public Zone currentZone() {
        return currentZone;
    }

    public void currentZone(Zone zone) {
        this.currentZone = zone;
    }

    public Zone selectedLevel() {
        return selectedLevel;
    }

    public void selectedLevel(Zone level) {
        this.selectedLevel = level;
    }

    public long levelStartedAt() {
        return levelStartedAt;
    }

    public void levelStartedAt(long millis) {
        this.levelStartedAt = millis;
    }

    private long completedDuration;

    public long completedDuration() {
        return completedDuration;
    }

    public void completedDuration(long millis) {
        this.completedDuration = millis;
    }

    public PlayerState savedState() {
        return savedState;
    }

    public void savedState(PlayerState state) {
        this.savedState = state;
    }

    public boolean hasCheckpoint() {
        return hasCheckpoint;
    }

    public int checkpointX() {
        return checkpointX;
    }

    public int checkpointY() {
        return checkpointY;
    }

    public int checkpointZ() {
        return checkpointZ;
    }

    public void setCheckpoint(int x, int y, int z) {
        this.checkpointX = x;
        this.checkpointY = y;
        this.checkpointZ = z;
        this.hasCheckpoint = true;
    }

    public void clearCheckpoint() {
        this.hasCheckpoint = false;
    }

    public boolean checkpointSoundEnabled() {
        return checkpointSoundEnabled;
    }

    public void checkpointSoundEnabled(boolean enabled) {
        this.checkpointSoundEnabled = enabled;
    }

    public boolean soundEnabled() {
        return soundEnabled;
    }

    public void soundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public boolean blockSoundEnabled() {
        return blockSoundEnabled;
    }

    public void blockSoundEnabled(boolean enabled) {
        this.blockSoundEnabled = enabled;
    }

    public VisibilityMode visibilityMode() {
        return visibilityMode;
    }

    public void visibilityMode(VisibilityMode mode) {
        this.visibilityMode = mode;
    }

    /** 总开关与存档点子项同时开启才播放存档点音效。 */
    public boolean shouldPlayCheckpointSound() {
        return soundEnabled && checkpointSoundEnabled;
    }

    /** 总开关与效果块子项同时开启才播放方块音效。 */
    public boolean shouldPlayBlockSound() {
        return soundEnabled && blockSoundEnabled;
    }

    public long lastMoveAt() {
        return lastMoveAt;
    }

    public double lastX() {
        return lastX;
    }

    public double lastY() {
        return lastY;
    }

    public double lastZ() {
        return lastZ;
    }

    public float lastYaw() {
        return lastYaw;
    }

    public float lastPitch() {
        return lastPitch;
    }

    /** 更新位置/视角快照（防挂机用）。返回是否有显著移动或视角变化。 */
    public boolean updateMovement(double x, double y, double z, float yaw, float pitch, long now,
                                  double moveThreshold, double rotThresholdDeg) {
        boolean moved = Math.abs(x - lastX) > moveThreshold
                || Math.abs(y - lastY) > moveThreshold
                || Math.abs(z - lastZ) > moveThreshold;
        double rotDelta = Math.abs(yaw - lastYaw) + Math.abs(pitch - lastPitch);
        boolean rotated = rotDelta > rotThresholdDeg;
        boolean active = moved || rotated;
        if (active) {
            this.lastMoveAt = now;
        }
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        return active;
    }

    public void initPosition(double x, double y, double z, float yaw, float pitch, long now) {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.lastMoveAt = now;
    }
}
