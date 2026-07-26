package dev.aaf.parkourArea.hotbar;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 快捷工具栏服务：进入全局区域时把工具填入 hotbar 9 格。
 *
 * <p>原物品由 {@link dev.aaf.parkourArea.player.PlayerState} 快照保存，离开时整体恢复。</p>
 */
public final class HotbarService {

    private final HotbarItems items;

    public HotbarService(Plugin plugin) {
        this.items = new HotbarItems(new NamespacedKey(plugin, "hotbar_action"));
    }

    /** 用工具填充热栏固定槽位（其余槽位由 PlayerState 快照原样保留）。 */
    public void applyTools(Player player) {
        player.getInventory().setItem(0, items.replay());
        player.getInventory().setItem(1, items.sound());
        player.getInventory().setItem(2, items.lobby());
        player.getInventory().setItem(4, items.checkpoint());
        player.getInventory().setItem(6, items.visibility());
        player.getInventory().setItem(8, items.menu());
        player.updateInventory();
    }

    public HotbarItems items() {
        return items;
    }
}
