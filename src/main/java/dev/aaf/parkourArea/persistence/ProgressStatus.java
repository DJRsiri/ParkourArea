package dev.aaf.parkourArea.persistence;

/** 玩家在某关的通关进度状态。 */
public enum ProgressStatus {
    /** 未到达过起点。 */
    NONE,
    /** 到达过起点（可挑战，菜单显示黄色）。 */
    VISITED,
    /** 已通关（菜单显示绿色）。 */
    COMPLETED;

    public static ProgressStatus parse(String s) {
        if (s == null) {
            return NONE;
        }
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
