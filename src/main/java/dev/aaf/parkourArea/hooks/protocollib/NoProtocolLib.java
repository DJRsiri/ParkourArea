package dev.aaf.parkourArea.hooks.protocollib;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** 未安装 ProtocolLib 或 scale 包不支持时的退化：任何非 1.0 缩放一律 hidePlayer。 */
public final class NoProtocolLib implements VisibilityHook {

    private final Plugin plugin;

    public NoProtocolLib(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean supportsScaling() {
        return false;
    }

    @Override
    public void applyScale(Player viewer, Player target, float scale) {
        viewer.hidePlayer(plugin, target);
    }

    @Override
    public void resetScale(Player viewer, Player target) {
        viewer.showPlayer(plugin, target);
    }
}
