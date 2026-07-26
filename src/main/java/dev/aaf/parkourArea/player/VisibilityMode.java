package dev.aaf.parkourArea.player;

/** 玩家在关卡区域内对其他跑酷玩家的可见性挡位。 */
public enum VisibilityMode {
    /** 完全可见。 */
    FULL,
    /** 玩家模型缩小为 x0.5。 */
    HALF,
    /** 玩家模型缩小为 x0.1。 */
    TENTH,
    /** 不可见。 */
    HIDDEN;

    /** 循环到下一个挡位（工具右键切换用）。 */
    public VisibilityMode next() {
        VisibilityMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static VisibilityMode parse(String s, VisibilityMode fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
