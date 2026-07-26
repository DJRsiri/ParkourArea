package dev.aaf.parkourArea.hooks.protocollib;

import org.bukkit.entity.Player;

/**
 * 玩家可见性/缩放钩子。
 *
 * <p>观察者侧：所有操作只影响 viewer 视角下对 target 的渲染，不影响 target 实际状态或第三方视角。</p>
 */
public interface VisibilityHook {

    /** 是否支持玩家模型缩放（ProtocolLib 缩小挡位）。不支持时 x0.5/x0.1 退化为隐藏。 */
    boolean supportsScaling();

    /** 把 target 在 viewer 视角下缩放（scale=1.0 等同还原）。 */
    void applyScale(Player viewer, Player target, float scale);

    /** 还原 target 在 viewer 视角下到默认（完全可见）。 */
    void resetScale(Player viewer, Player target);
}
