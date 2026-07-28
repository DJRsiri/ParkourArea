package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.persistence.PlayerTimeDao;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用时排行榜菜单（总览 OVERVIEW / 单关详细 DETAIL 双视图）。
 *
 * <p><b>OVERVIEW</b>：当前 GLOBAL 区域内每个关卡一个物品（三色混凝土，45 格/页翻页），
 * lore=我的最佳 + 全服前 3 名；顶部：上一页 / GLOBAL 切换（单 GLOBAL 隐藏）/
 * 区域+页码指示 / 返回主菜单 / 下一页。</p>
 *
 * <p><b>DETAIL</b>（点关卡进入）：槽 20/21/23/24 四个摘要物品 lore 展开——
 * 我的最佳（独立 best 表，绝不从最近挑战推算）/ 最近 10 次 / 全服第 1-10 名 /
 * 全服第 11-20 名；顶部：关卡指示 / 返回总览。</p>
 */
public final class LeaderboardMenu extends ParkourMenu {

    private enum View { OVERVIEW, DETAIL }

    private static final int PAGE_SIZE = 45;          // 内容区槽 9-53
    private static final int CONTENT_START = 9;
    private static final int SLOT_PREV = 0;
    private static final int SLOT_GLOBAL_SWITCH = 2;
    private static final int SLOT_INDICATOR = 4;
    private static final int SLOT_BACK = 6;
    private static final int SLOT_NEXT = 8;
    private static final int[] DETAIL_SLOTS = {20, 21, 23, 24};

    private View view = View.OVERVIEW;
    private int currentGlobalId;
    private int page;
    private int detailLevelId;
    private final Map<Integer, Integer> slotLevels = new HashMap<>(); // OVERVIEW 槽位→levelId

    public LeaderboardMenu(ParkourArea plugin, Player viewer) {
        super(plugin, viewer, plugin.messages().raw("menu.leaderboard-title"), 54);
        this.currentGlobalId = inferGlobal(viewer);
    }

    /** 玩家所在 GLOBAL；不在任何 GLOBAL 内则 id 最小的 GLOBAL；无 GLOBAL → -1。 */
    private int inferGlobal(Player viewer) {
        var loc = viewer.getLocation();
        if (loc.getWorld() != null) {
            for (Zone z : plugin.zoneRepository().tree().findChain(
                    loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ())) {
                if (z.type() == ZoneType.GLOBAL) {
                    return z.id();
                }
            }
        }
        return allGlobals().stream().mapToInt(Zone::id).min().orElse(-1);
    }

    private List<Zone> allGlobals() {
        return plugin.zoneRepository().tree().all().stream()
                .filter(z -> z.type() == ZoneType.GLOBAL)
                .sorted(Comparator.comparingInt(Zone::id))
                .toList();
    }

    /** 当前 GLOBAL 的 LEVEL 子区域（id 升序）。 */
    private List<Zone> levelsOfCurrentGlobal() {
        if (currentGlobalId < 0) {
            return List.of();
        }
        return plugin.zoneRepository().tree().childrenOf(currentGlobalId).stream()
                .filter(z -> z.type() == ZoneType.LEVEL)
                .sorted(Comparator.comparingInt(Zone::id))
                .toList();
    }

    @Override
    public void render() {
        inventory.clear();
        slotLevels.clear();
        if (view == View.OVERVIEW) {
            renderOverview();
        } else {
            renderDetail();
        }
    }

    // ---------- OVERVIEW ----------

    private void renderOverview() {
        List<Zone> globals = allGlobals();
        if (currentGlobalId < 0 || globals.isEmpty()) {
            inventory.setItem(SLOT_INDICATOR, icon(Material.BARRIER,
                    plugin.messages().raw("menu.lb-no-global")));
            inventory.setItem(SLOT_BACK, icon(Material.BARRIER,
                    plugin.messages().raw("menu.lb-back")));
            return;
        }
        Zone global = plugin.zoneRepository().tree().getById(currentGlobalId);
        if (global == null) {
            currentGlobalId = globals.get(0).id();
            global = globals.get(0);
        }
        String globalName = global.name().isEmpty() ? "#" + global.id() : global.name();

        List<Zone> levels = levelsOfCurrentGlobal();
        int pages = Math.max(1, (levels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }

        // 控制行
        inventory.setItem(SLOT_PREV, icon(pages > 1 ? Material.ARROW
                : Material.GRAY_STAINED_GLASS_PANE, plugin.messages().raw("menu.lb-prev")));
        inventory.setItem(SLOT_NEXT, icon(pages > 1 ? Material.ARROW
                : Material.GRAY_STAINED_GLASS_PANE, plugin.messages().raw("menu.lb-next")));
        if (globals.size() > 1) {
            inventory.setItem(SLOT_GLOBAL_SWITCH, icon(Material.COMPASS,
                    plugin.messages().raw("menu.lb-global-switch", Map.of("name", globalName)),
                    plugin.messages().raw("menu.lb-global-switch-lore")));
        }
        inventory.setItem(SLOT_INDICATOR, icon(Material.CLOCK,
                plugin.messages().raw("menu.lb-page-indicator", Map.of(
                        "name", globalName,
                        "page", String.valueOf(page + 1),
                        "pages", String.valueOf(pages)))));
        inventory.setItem(SLOT_BACK, icon(Material.BARRIER,
                plugin.messages().raw("menu.lb-back")));

        // 内容区：当前页关卡
        UUID uid = viewer.getUniqueId();
        int nextExpected = plugin.progressService().firstNonCompletedLevelId(uid, currentGlobalId);
        int from = page * PAGE_SIZE;
        int to = Math.min(levels.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) {
            Zone level = levels.get(i);
            int slot = CONTENT_START + (i - from);
            inventory.setItem(slot, levelIcon(uid, level, nextExpected));
            slotLevels.put(slot, level.id());
        }
    }

    /** 关卡物品：三色混凝土（沿用选关菜单状态）+ lore（我的最佳 + 全服前 3）。 */
    private org.bukkit.inventory.ItemStack levelIcon(UUID uid, Zone level, int nextExpected) {
        ProgressStatus status = plugin.progressService().getStatus(uid, level.id());
        Material material;
        String nameKey;
        if (status == ProgressStatus.COMPLETED) {
            material = Material.GREEN_CONCRETE;
            nameKey = "menu.lb-level-green";
        } else if (status == ProgressStatus.VISITED || level.id() == nextExpected) {
            material = Material.YELLOW_CONCRETE;
            nameKey = "menu.lb-level-yellow";
        } else {
            material = Material.GRAY_CONCRETE;
            nameKey = "menu.lb-level-grey";
        }
        String levelName = level.name().isEmpty() ? "#" + level.id() : level.name();

        List<String> lore = new ArrayList<>();
        Long best = plugin.timerService().getBest(uid, level.id());
        lore.add(best == null
                ? plugin.messages().raw("menu.lb-overview-no-best")
                : plugin.messages().raw("menu.lb-overview-best",
                        Map.of("time", ActionBarService.formatDuration(best))));
        List<PlayerTimeDao.TimeEntry> top3 = topTimes(level.id(), 3);
        if (top3.isEmpty()) {
            lore.add(plugin.messages().raw("menu.lb-no-record"));
        } else {
            lore.add(plugin.messages().raw("menu.lb-overview-top3-header"));
            for (int r = 0; r < top3.size(); r++) {
                PlayerTimeDao.TimeEntry e = top3.get(r);
                lore.add(plugin.messages().raw("menu.lb-rank", Map.of(
                        "rank", String.valueOf(r + 1),
                        "time", ActionBarService.formatDuration(e.duration()),
                        "player", resolveName(e.player()))));
            }
        }
        return icon(material, plugin.messages().raw(nameKey, Map.of("name", levelName)),
                lore.toArray(new String[0]));
    }

    // ---------- DETAIL ----------

    private void renderDetail() {
        Zone level = plugin.zoneRepository().tree().getById(detailLevelId);
        if (level == null) {
            view = View.OVERVIEW;
            renderOverview();
            return;
        }
        String levelName = level.name().isEmpty() ? "#" + level.id() : level.name();
        inventory.setItem(SLOT_INDICATOR, icon(Material.CLOCK,
                plugin.messages().raw("menu.lb-detail-indicator", Map.of("name", levelName))));
        inventory.setItem(SLOT_BACK, icon(Material.ARROW,
                plugin.messages().raw("menu.lb-back-overview")));

        UUID uid = viewer.getUniqueId();
        // 1) 我的最佳（独立 best 表，绝不从最近挑战推算）
        Long best = plugin.timerService().getBest(uid, detailLevelId);
        inventory.setItem(DETAIL_SLOTS[0], icon(Material.GOLDEN_APPLE,
                plugin.messages().raw("menu.lb-my-best"),
                best == null
                        ? plugin.messages().raw("menu.lb-no-record")
                        : "&a" + ActionBarService.formatDuration(best)));
        // 2) 最近 10 次（n=1 最新）
        List<Long> recent = recentTimes(uid, detailLevelId, 10);
        List<String> recentLore = new ArrayList<>();
        if (recent.isEmpty()) {
            recentLore.add(plugin.messages().raw("menu.lb-no-record"));
        } else {
            for (int i = 0; i < recent.size(); i++) {
                recentLore.add(plugin.messages().raw("menu.lb-self-rank", Map.of(
                        "n", String.valueOf(i + 1),
                        "time", ActionBarService.formatDuration(recent.get(i)))));
            }
        }
        inventory.setItem(DETAIL_SLOTS[1], icon(Material.BOOK,
                plugin.messages().raw("menu.lb-detail-recent"),
                recentLore.toArray(new String[0])));
        // 3)+4) 全服第 1-10 / 11-20 名
        List<PlayerTimeDao.TimeEntry> top = topTimes(detailLevelId, 20);
        inventory.setItem(DETAIL_SLOTS[2], icon(Material.PAPER,
                plugin.messages().raw("menu.lb-detail-top10"),
                rankLore(top, 0, 10)));
        inventory.setItem(DETAIL_SLOTS[3], icon(Material.PAPER,
                plugin.messages().raw("menu.lb-detail-top20"),
                rankLore(top, 10, 20)));
    }

    /** 全服排行 lore 行（[from,to) 名，rank 从 from+1 起）；空段返回"暂无"。 */
    private String[] rankLore(List<PlayerTimeDao.TimeEntry> top, int from, int to) {
        List<String> lore = new ArrayList<>();
        for (int i = from; i < Math.min(top.size(), to); i++) {
            PlayerTimeDao.TimeEntry e = top.get(i);
            lore.add(plugin.messages().raw("menu.lb-rank", Map.of(
                    "rank", String.valueOf(i + 1),
                    "time", ActionBarService.formatDuration(e.duration()),
                    "player", resolveName(e.player()))));
        }
        if (lore.isEmpty()) {
            lore.add(plugin.messages().raw("menu.lb-no-record"));
        }
        return lore.toArray(new String[0]);
    }

    // ---------- 点击 ----------

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (view == View.DETAIL) {
            if (slot == SLOT_BACK) {
                view = View.OVERVIEW;
                refresh();
            }
            return;
        }
        // OVERVIEW
        List<Zone> levels = levelsOfCurrentGlobal();
        int pages = Math.max(1, (levels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (slot == SLOT_PREV) {
            if (page > 0) {
                page--;
                refresh();
            }
        } else if (slot == SLOT_NEXT) {
            if (page < pages - 1) {
                page++;
                refresh();
            }
        } else if (slot == SLOT_GLOBAL_SWITCH) {
            List<Zone> globals = allGlobals();
            if (globals.size() > 1) {
                int idx = -1;
                for (int i = 0; i < globals.size(); i++) {
                    if (globals.get(i).id() == currentGlobalId) {
                        idx = i;
                        break;
                    }
                }
                currentGlobalId = globals.get((idx + 1) % globals.size()).id();
                page = 0;
                refresh();
            }
        } else if (slot == SLOT_BACK) {
            new MainMenu(plugin, viewer).open();
        } else {
            Integer levelId = slotLevels.get(slot);
            if (levelId != null) {
                detailLevelId = levelId;
                view = View.DETAIL;
                refresh();
            }
        }
    }

    private void refresh() {
        render();
        viewer.updateInventory();
    }

    // ---------- 数据访问（同步 DAO，与项目现有菜单同策略）----------

    private List<Long> recentTimes(UUID uid, int levelId, int limit) {
        try {
            return plugin.timeDao().getRecentTimes(uid, levelId, limit);
        } catch (Exception e) {
            plugin.getLogger().warning("读取最近挑战记录失败: " + e.getMessage());
            return List.of();
        }
    }

    private List<PlayerTimeDao.TimeEntry> topTimes(int levelId, int limit) {
        try {
            return plugin.timeDao().getTopTimesForLevel(levelId, limit);
        } catch (Exception e) {
            plugin.getLogger().warning("读取全服排行失败: " + e.getMessage());
            return List.of();
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
}
