package dev.aaf.parkourArea.hooks.protocollib;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 装了 ProtocolLib 时的实装。构造时探测 scale 包能力；运行时发包失败也降级为 hidePlayer。
 */
public final class ProtocolLibPresent implements VisibilityHook {

    private final Plugin plugin;
    private final boolean scalingSupported;

    public ProtocolLibPresent(Plugin plugin) {
        this.plugin = plugin;
        this.scalingSupported = ScalePackets.probe();
    }

    @Override
    public boolean supportsScaling() {
        return scalingSupported;
    }

    @Override
    public void applyScale(Player viewer, Player target, float scale) {
        if (scalingSupported) {
            try {
                ScalePackets.sendScale(viewer, target, scale);
                return;
            } catch (Throwable t) {
                // 发包失败，退化为隐藏
            }
        }
        viewer.hidePlayer(plugin, target);
    }

    @Override
    public void resetScale(Player viewer, Player target) {
        if (scalingSupported) {
            try {
                ScalePackets.sendReset(viewer, target);
            } catch (Throwable t) {
                // 还原失败回退 showPlayer
            }
        }
        viewer.showPlayer(plugin, target);
    }
}
