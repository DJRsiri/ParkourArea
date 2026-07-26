package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import dev.aaf.parkourArea.zone.Zone;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * 最佳用时菜单：按关卡顺序列出玩家在各关的最短用时（已通关的关显示最佳，未通关显示灰色）。
 * 只读菜单。
 */
public final class LeaderboardMenu extends ParkourMenu {

    public LeaderboardMenu(ParkourArea plugin, Player viewer) {
        super(plugin, viewer, plugin.messages().raw("menu.leaderboard-title"), 54);
    }

    @Override
    public void render() {
        inventory.clear();
        List<Integer> levelIds = plugin.progressService().sortedLevelIds();
        int slot = 0;
        for (int levelId : levelIds) {
            Zone level = plugin.zoneRepository().tree().getById(levelId);
            if (level == null) {
                continue;
            }
            String levelName = level.name().isEmpty() ? "#" + levelId : level.name();
            Long best = plugin.timerService().getBest(viewer.getUniqueId(), levelId);
            ProgressStatus status = plugin.progressService().getStatus(viewer.getUniqueId(), levelId);
            String title = (slot + 1) + ". " + levelName;
            if (status == ProgressStatus.COMPLETED) {
                title = "&a" + title;
            } else if (status == ProgressStatus.VISITED) {
                title = "&e" + title;
            } else {
                title = "&7" + title;
            }
            String bestLore = best == null ? "&8未通关" : "&7最佳: &e" + ActionBarService.formatDuration(best);
            inventory.setItem(slot, icon(Material.PAPER, title, bestLore));
            slot++;
            if (slot >= 45) {
                break;
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        // 只读菜单，无操作
    }
}
