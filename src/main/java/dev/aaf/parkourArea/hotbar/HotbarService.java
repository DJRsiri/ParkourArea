package dev.aaf.parkourArea.hotbar;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 快捷工具栏服务：进入全局区域时把工具填入 hotbar 9 格。
 *
 * <p>原物品由 {@link dev.aaf.parkourArea.player.PlayerState} 全量快照 9 格
 * （含空槽位），离开/退出时逐格恢复；填入工具前先清空整条热栏，
 * 保证无工具槽位不留玩家物品（防止跑酷中使用自带物品）。</p>
 */
public final class HotbarService {

    private final HotbarItems items;

    public HotbarService(Plugin plugin) {
        this.items = new HotbarItems(new NamespacedKey(plugin, "hotbar_action"));
    }

    /** 清空热栏 9 格后用工具填充固定槽位（原物品由 PlayerState 快照保存，离开时整体恢复）。 */
    public void applyTools(Player player) {
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, null);
        }
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
