package dev.aaf.parkourArea.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 玩家进入跑酷区域前的原始状态快照。
 *
 * <p>快照热栏 9 格 + 游戏模式 + 飞行状态；离开区域或进入编辑模式时恢复。</p>
 */
public final class PlayerState {

    private final GameMode gameMode;
    private final ItemStack[] hotbar; // 长度 9
    private final boolean allowFlight;
    private final boolean flying;

    private PlayerState(GameMode gameMode, ItemStack[] hotbar, boolean allowFlight, boolean flying) {
        this.gameMode = gameMode;
        this.hotbar = hotbar;
        this.allowFlight = allowFlight;
        this.flying = flying;
    }

    public static PlayerState snapshot(Player player) {
        ItemStack[] hotbar = new ItemStack[9];
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack it = inv.getItem(i);
            hotbar[i] = it == null ? null : it.clone();
        }
        return new PlayerState(player.getGameMode(), hotbar, player.getAllowFlight(), player.isFlying());
    }

    /** 恢复玩家原始状态（含热栏）。须在玩家线程调用。 */
    public void restore(Player player) {
        player.setGameMode(gameMode);
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, hotbar[i] == null ? null : hotbar[i].clone());
        }
        player.setAllowFlight(allowFlight);
        if (allowFlight) {
            player.setFlying(flying);
        }
        player.updateInventory();
    }

    public GameMode gameMode() {
        return gameMode;
    }
}
