package dev.aaf.parkourArea.player;

/** 取消跑酷标记的原因（决定提示消息）。 */
public enum UnmarkReason {
    /** 走出全局区域。 */
    LEFT_ZONE,
    /** 切换到非指定游戏模式。 */
    GAMEMODE,
    /** 进入编辑模式。 */
    EDIT_MODE
}
