package dev.aaf.parkourArea.player;

/** 玩家在跑酷区域内的状态机阶段。 */
public enum PlayerPhase {
    /** 不在全局区域内。 */
    OUTSIDE,
    /** 在大厅区域内。 */
    IN_LOBBY,
    /** 已选关（等待传送/到达起点）。 */
    LEVEL_SELECTED,
    /** 踩在所选关的起点（已登记 VISITED）。 */
    AT_START,
    /** 离开起点，正在游玩。 */
    RUNNING,
    /** 触发中途存档点（仍属于 RUNNING 子相位）。 */
    AT_CHECKPOINT,
    /** 已通关当前关。 */
    COMPLETED,
    /** 防挂机/跳关触发，将被传回大厅。 */
    INVALIDATED,
    /** 编辑模式（不触发跑酷交互）。 */
    EDIT_MODE
}
