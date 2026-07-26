package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** 主菜单：选关入口、最佳用时入口。 */
public final class MainMenu extends ParkourMenu {

    private static final int SLOT_SELECT = 11;
    private static final int SLOT_LEADERBOARD = 15;

    public MainMenu(ParkourArea plugin, Player viewer) {
        super(plugin, viewer, plugin.messages().raw("menu.main-title"), 27);
    }

    @Override
    public void render() {
        inventory.clear();
        inventory.setItem(SLOT_SELECT, icon(Material.BOOK, "&a选择关卡", "&7点击选择要挑战的关卡"));
        inventory.setItem(SLOT_LEADERBOARD, icon(Material.GOLDEN_APPLE, "&e最佳用时", "&7查看你各关卡的最短用时"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == SLOT_SELECT) {
            new LevelSelectMenu(plugin, viewer).open();
        } else if (slot == SLOT_LEADERBOARD) {
            new LeaderboardMenu(plugin, viewer).open();
        }
    }
}
