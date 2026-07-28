package dev.aaf.parkourArea.persistence;

/** 玩家在某关的通关进度状态。 */
public enum ProgressStatus {
    /** 未到达过起点。 */
    NONE,
    /** 到达过起点（可挑战，菜单显示黄色）。 */
    VISITED,
    /** 已通关（菜单显示绿色）。 */
    COMPLETED;

    /** 到访起点的状态迁移：已通关不退化（保持 COMPLETED），其余变为 VISITED。 */
    public ProgressStatus onVisit() {
        return this == COMPLETED ? COMPLETED : VISITED;
    }

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
