package dev.aaf.parkourArea.persistence;

import dev.aaf.parkourArea.player.VisibilityMode;

/** 玩家偏好（音效三层开关 + 可见性挡位）值对象。 */
public record Preference(boolean sound, boolean checkpoint, boolean block, VisibilityMode mode) {
}
