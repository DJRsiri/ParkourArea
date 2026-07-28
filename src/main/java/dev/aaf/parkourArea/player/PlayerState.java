package dev.aaf.parkourArea.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 玩家进入跑酷区域前的原始状态快照。
 *
 * <p>仅快照热栏 9 格——热栏是唯一被插件主动替换（{@code applyTools}）因而需要还原的状态；
 * 离开区域或进入编辑模式时恢复。</p>
 *
 * <p>游戏模式与飞行能力（allowFlight/flying）均不参与快照/恢复：插件从不主动修改它们，
 * 切换游戏模式时服务端会自动设置该模式的默认飞行能力，玩家/其他插件的授权也归其自管。
 * 若用标记时刻的旧值回写，会覆盖新模式上下文下服务端已设好的能力
 * （如切旁观/创造后被对账 unmark，飞行能力被错误清除）。</p>
 */
public final class PlayerState {

    private final ItemStack[] hotbar; // 长度 9

    private PlayerState(ItemStack[] hotbar) {
        this.hotbar = hotbar;
    }

    public static PlayerState snapshot(Player player) {
        ItemStack[] hotbar = new ItemStack[9];
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack it = inv.getItem(i);
            hotbar[i] = it == null ? null : it.clone();
        }
        return new PlayerState(hotbar);
    }

    /** 恢复玩家原始热栏。须在玩家线程调用。 */
    public void restore(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, hotbar[i] == null ? null : hotbar[i].clone());
        }
        player.updateInventory();
    }
}
