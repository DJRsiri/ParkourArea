package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.persistence.PlayerTimeDao;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用时排行榜菜单（单关双视图 + 翻页切关）。
 *
 * <p>进入默认显示玩家当前所在关卡（无则第一关），两种视图可切换：
 * <ul>
 *   <li><b>我的记录 (SELF)</b>：第一条=自己最佳（独立维护，不从最近挑战推算），
 *       往下 10 条=最近 10 次挑战（最新在前），不足显示"暂无"。</li>
 *   <li><b>全服记录 (GLOBAL)</b>：前 20 条最快记录（同一玩家可多次上榜），不足显示"暂无"。</li>
 * </ul>
 * 顶部控制行：上一关 / 切视图 / 当前关卡指示 / 返回主菜单 / 下一关。</p>
 */
public final class LeaderboardMenu extends ParkourMenu {

    private enum ViewMode { SELF, GLOBAL }

    private static final int SLOT_PREV = 0;
    private static final int SLOT_TOGGLE = 2;
    private static final int SLOT_INDICATOR = 4;
    private static final int SLOT_BACK = 6;
    private static final int SLOT_NEXT = 8;

    private int currentLevelId;
    private ViewMode viewMode;

    public LeaderboardMenu(ParkourArea plugin, Player viewer) {
        super(plugin, viewer, plugin.messages().raw("menu.leaderboard-title"), 54);
        this.viewMode = ViewMode.SELF;
        this.currentLevelId = inferDefaultLevel(viewer);
    }

    private int inferDefaultLevel(Player viewer) {
        var loc = viewer.getLocation();
        if (loc.getWorld() != null) {
            for (Zone z : plugin.zoneRepository().tree().findChain(
                    loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ())) {
                if (z.type() == ZoneType.LEVEL) {
                    return z.id();
                }
            }
        }
        List<Integer> ids = plugin.progressService().sortedLevelIds();
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    @Override
    public void render() {
        inventory.clear();
        List<Integer> levelIds = plugin.progressService().sortedLevelIds();
        if (levelIds.isEmpty() || currentLevelId < 0) {
            inventory.setItem(SLOT_INDICATOR, icon(Material.BARRIER, "&c暂无关卡"));
            return;
        }
        if (!levelIds.contains(currentLevelId)) {
            currentLevelId = levelIds.get(0);
        }
        int idx = levelIds.indexOf(currentLevelId);
        Zone level = plugin.zoneRepository().tree().getById(currentLevelId);
        String levelName = (level == null || level.name().isEmpty())
                ? "#" + currentLevelId : level.name();

        // 控制行
        inventory.setItem(SLOT_PREV, icon(Material.ARROW,
                plugin.messages().raw("menu.lb-prev")));
        inventory.setItem(SLOT_NEXT, icon(Material.ARROW,
                plugin.messages().raw("menu.lb-next")));
        inventory.setItem(SLOT_TOGGLE, icon(
                viewMode == ViewMode.SELF ? Material.PLAYER_HEAD : Material.BOOK,
                plugin.messages().raw(viewMode == ViewMode.SELF
                        ? "menu.lb-to-global" : "menu.lb-to-self")));
        inventory.setItem(SLOT_INDICATOR, icon(Material.CLOCK,
                plugin.messages().raw("menu.lb-level-indicator",
                        Map.of("index", String.valueOf(idx + 1), "name", levelName)),
                plugin.messages().raw(viewMode == ViewMode.SELF
                        ? "menu.lb-my-best" : "menu.lb-global-header")));
        inventory.setItem(SLOT_BACK, icon(Material.BARRIER,
                plugin.messages().raw("menu.lb-back")));

        if (viewMode == ViewMode.SELF) {
            renderSelf();
        } else {
            renderGlobal();
        }
    }

    private void renderSelf() {
        UUID uid = viewer.getUniqueId();
        Long best = plugin.timerService().getBest(uid, currentLevelId);
        List<Long> recent = new ArrayList<>();
        try {
            recent = plugin.timeDao().getRecentTimes(uid, currentLevelId, 10);
        } catch (Exception e) {
            plugin.getLogger().warning("读取最近挑战记录失败: " + e.getMessage());
        }
        // 槽 9：我的最佳（独立于最近挑战记录）
        inventory.setItem(9, icon(Material.GOLDEN_APPLE,
                plugin.messages().raw("menu.lb-my-best"),
                best == null
                        ? plugin.messages().raw("menu.lb-no-record")
                        : "&a" + ActionBarService.formatDuration(best)));
        // 槽 10：最近挑战标签
        inventory.setItem(10, icon(Material.BOOK,
                plugin.messages().raw("menu.lb-recent-header")));
        // 槽 11-20：最近 10 次（n=1 最新）
        for (int i = 0; i < 10; i++) {
            int slot = 11 + i;
            if (i < recent.size()) {
                inventory.setItem(slot, icon(Material.PAPER,
                        plugin.messages().raw("menu.lb-self-rank", Map.of(
                                "n", String.valueOf(i + 1),
                                "time", ActionBarService.formatDuration(recent.get(i))))));
            } else {
                inventory.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE,
                        plugin.messages().raw("menu.lb-no-record")));
            }
        }
    }

    private void renderGlobal() {
        List<PlayerTimeDao.TimeEntry> top = new ArrayList<>();
        try {
            top = plugin.timeDao().getTopTimesForLevel(currentLevelId, 20);
        } catch (Exception e) {
            plugin.getLogger().warning("读取全服排行失败: " + e.getMessage());
        }
        inventory.setItem(9, icon(Material.BOOK,
                plugin.messages().raw("menu.lb-global-header")));
        // 槽 10-29：前 20 条最快记录（rank=1 最快）
        for (int i = 0; i < 20; i++) {
            int slot = 10 + i;
            if (i < top.size()) {
                PlayerTimeDao.TimeEntry entry = top.get(i);
                inventory.setItem(slot, icon(Material.PAPER,
                        plugin.messages().raw("menu.lb-rank", Map.of(
                                "rank", String.valueOf(i + 1),
                                "time", ActionBarService.formatDuration(entry.duration()),
                                "player", resolveName(entry.player())))));
            } else {
                inventory.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE,
                        plugin.messages().raw("menu.lb-no-record")));
            }
        }
    }

    private String resolveName(UUID uid) {
        if (uid == null) {
            return "??";
        }
        Player online = Bukkit.getPlayer(uid);
        if (online != null) {
            return online.getName() != null ? online.getName() : "??";
        }
        String name = Bukkit.getOfflinePlayer(uid).getName();
        return name != null ? name : "??";
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        List<Integer> levelIds = plugin.progressService().sortedLevelIds();
        if (levelIds.isEmpty()) {
            return;
        }
        int idx = levelIds.indexOf(currentLevelId);
        if (slot == SLOT_PREV) {
            if (idx > 0) {
                currentLevelId = levelIds.get(idx - 1);
                refresh();
            }
        } else if (slot == SLOT_NEXT) {
            if (idx >= 0 && idx < levelIds.size() - 1) {
                currentLevelId = levelIds.get(idx + 1);
                refresh();
            }
        } else if (slot == SLOT_TOGGLE) {
            viewMode = (viewMode == ViewMode.SELF) ? ViewMode.GLOBAL : ViewMode.SELF;
            refresh();
        } else if (slot == SLOT_BACK) {
            new MainMenu(plugin, viewer).open();
        }
    }

    private void refresh() {
        render();
        viewer.updateInventory();
    }
}
