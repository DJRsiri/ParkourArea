package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
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
 * 选关菜单：三色混凝土区分关卡状态，按 GLOBAL 区域分页展示（45 格/页，布局与排行榜一致）。
 *
 * <ul>
 *   <li>NONE（未到起点、未解锁）：灰色混凝土，不可选</li>
 *   <li>VISITED 或 nextExpected（或自由选关模式）：黄色混凝土，可选，显示最佳用时（无则"暂无"）</li>
 *   <li>COMPLETED：绿色混凝土，可选，显示最佳用时</li>
 * </ul>
 * 顶部控制行：上一页 / GLOBAL 切换（单 GLOBAL 隐藏）/ 区域+页码指示 / 返回主菜单 / 下一页。
 */
public final class LevelSelectMenu extends ParkourMenu {

    private static final int PAGE_SIZE = 45;          // 内容区槽 9-53
    private static final int CONTENT_START = 9;
    private static final int SLOT_PREV = 0;
    private static final int SLOT_GLOBAL_SWITCH = 2;
    private static final int SLOT_INDICATOR = 4;
    private static final int SLOT_BACK = 6;
    private static final int SLOT_NEXT = 8;

    private int currentGlobalId;
    private int page;
    private final Map<Integer, Zone> selectableSlots = new HashMap<>(); // 可选关卡槽位
    private final Map<Integer, Zone> levelSlots = new HashMap<>();      // 全部关卡槽位（含灰色）

    public LevelSelectMenu(ParkourArea plugin, Player viewer) {
        super(plugin, viewer, plugin.messages().raw("menu.level-select-title"), 54);
        this.currentGlobalId = inferGlobal(viewer);
    }

    /** 玩家所在 GLOBAL（按会话当前区域）；不在任何 GLOBAL 内则 id 最小的 GLOBAL；无 GLOBAL → -1。 */
    private int inferGlobal(Player viewer) {
        ParkourPlayer session = plugin.sessionService().get(viewer.getUniqueId());
        if (session != null && session.currentZone() != null) {
            int g = plugin.zoneRepository().tree().globalOf(session.currentZone().id());
            if (g != -1) {
                return g;
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
        selectableSlots.clear();
        levelSlots.clear();
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

        // 控制行（与排行榜一致）
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

        // 内容区：当前页关卡（三色状态）
        UUID uid = viewer.getUniqueId();
        int nextExpected = plugin.progressService().firstNonCompletedLevelId(uid, currentGlobalId);
        boolean anySelectable = plugin.configService().settings().allowAnySelectable();
        int from = page * PAGE_SIZE;
        int to = Math.min(levels.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) {
            Zone level = levels.get(i);
            int slot = CONTENT_START + (i - from);
            ProgressStatus status = plugin.progressService().getStatus(uid, level.id());
            Material material;
            String nameKey;
            String loreKey;
            boolean selectable;
            if (status == ProgressStatus.COMPLETED) {
                material = Material.GREEN_CONCRETE;
                nameKey = "menu.level-green-name";
                loreKey = "menu.lore-status-green";
                selectable = true;
            } else if (status == ProgressStatus.VISITED || level.id() == nextExpected || anySelectable) {
                material = Material.YELLOW_CONCRETE;
                nameKey = "menu.level-yellow-name";
                loreKey = "menu.lore-status-yellow";
                selectable = true;
            } else {
                material = Material.GRAY_CONCRETE;
                nameKey = "menu.level-grey-name";
                loreKey = "menu.lore-status-grey";
                selectable = false;
            }
            String levelName = level.name().isEmpty() ? "#" + level.id() : level.name();
            Map<String, String> vars = new HashMap<>();
            vars.put("index", String.valueOf(i + 1));
            vars.put("name", levelName);
            List<String> lore = new ArrayList<>();
            lore.add(plugin.messages().raw(loreKey, vars));
            if (selectable) {
                Long best = plugin.timerService().getBest(uid, level.id());
                lore.add(best == null
                        ? plugin.messages().raw("menu.lore-no-best")
                        : plugin.messages().raw("menu.lore-best", Map.of(
                                "best", ActionBarService.formatDuration(best))));
            }
            inventory.setItem(slot, icon(material, plugin.messages().raw(nameKey, vars),
                    lore.toArray(new String[0])));
            levelSlots.put(slot, level);
            if (selectable) {
                selectableSlots.put(slot, level);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        List<Zone> levels = levelsOfCurrentGlobal();
        int pages = Math.max(1, (levels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (slot == SLOT_PREV) {
            if (page > 0) {
                page--;
                refresh();
            }
            return;
        }
        if (slot == SLOT_NEXT) {
            if (page < pages - 1) {
                page++;
                refresh();
            }
            return;
        }
        if (slot == SLOT_GLOBAL_SWITCH) {
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
            return;
        }
        if (slot == SLOT_BACK) {
            new MainMenu(plugin, viewer).open();
            return;
        }
        Zone level = selectableSlots.get(slot);
        if (level == null) {
            if (levelSlots.containsKey(slot)) {
                plugin.messages().send(viewer, "command.invalid-hierarchy",
                        Map.of("reason", "该关卡未解锁"));
            }
            return;
        }
        viewer.closeInventory();
        selectLevel(level);
    }

    private void refresh() {
        render();
        viewer.updateInventory();
    }

    private void selectLevel(Zone level) {
        ParkourPlayer session = plugin.sessionService().get(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        Zone start = findStart(level);
        if (start == null) {
            plugin.messages().send(viewer, "command.not-found", Map.of("query", "起点区域"));
            return;
        }
        session.selectedLevel(level);
        session.phase(PlayerPhase.LEVEL_SELECTED);
        session.clearCheckpoint();
        plugin.scheduler().runEntity(viewer, p -> {
            org.bukkit.World world = start.worldUid() != null
                    ? org.bukkit.Bukkit.getWorld(start.worldUid()) : p.getWorld();
            if (world != null) {
                // 坐标取 START（spawn 显式值，缺省取区域中心+最高非空气方块）；
                // 朝向取 LEVEL spawn 优先，未指定字段按配置保留玩家当前朝向或回落 0/0
                p.teleport(dev.aaf.parkourArea.util.Locations.teleportLocation(world, start, level,
                        plugin.configService().settings().teleportKeepRotation(), p.getLocation()));
            }
        }, () -> {});
    }

    private Zone findStart(Zone level) {
        for (Zone c : plugin.zoneRepository().tree().childrenOf(level.id())) {
            if (c.type() == ZoneType.START) {
                return c;
            }
        }
        return null;
    }
}
