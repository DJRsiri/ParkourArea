package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.zone.SelectionShape;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选关菜单：按通关状态显示灰/黄/绿混凝土。
 *
 * <ul>
 *   <li>NONE（未到起点、未解锁）：灰色混凝土，不可选</li>
 *   <li>VISITED 或 nextExpected：黄色混凝土，可选</li>
 *   <li>COMPLETED：绿色混凝土，可选，显示最佳用时</li>
 * </ul>
 * 玩家只能点击非灰色混凝土的关卡。
 */
public final class LevelSelectMenu extends ParkourMenu {

    private final Map<Integer, Zone> selectableSlots = new HashMap<>();

    public LevelSelectMenu(ParkourArea plugin, Player viewer) {
        super(plugin, viewer, plugin.messages().raw("menu.level-select-title"), 54);
    }

    @Override
    public void render() {
        inventory.clear();
        selectableSlots.clear();
        ParkourPlayer session = plugin.sessionService().get(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        int nextExpected = plugin.progressService().firstNonCompletedLevelId(viewer.getUniqueId());
        List<Integer> levelIds = plugin.progressService().sortedLevelIds();
        int slot = 0;
        for (int levelId : levelIds) {
            Zone level = plugin.zoneRepository().tree().getById(levelId);
            if (level == null) {
                continue;
            }
            ProgressStatus status = plugin.progressService().getStatus(viewer.getUniqueId(), levelId);
            Material material;
            String nameKey;
            String loreKey;
            boolean selectable;
            if (status == ProgressStatus.COMPLETED) {
                material = Material.GREEN_CONCRETE;
                nameKey = "menu.level-green-name";
                loreKey = "menu.lore-status-green";
                selectable = true;
            } else if (status == ProgressStatus.VISITED || levelId == nextExpected) {
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
            vars.put("index", String.valueOf(slot + 1));
            vars.put("name", levelName);
            List<String> lore = new ArrayList<>();
            lore.add(plugin.messages().raw(loreKey, vars));
            if (status == ProgressStatus.COMPLETED) {
                Long best = plugin.timerService().getBest(viewer.getUniqueId(), levelId);
                lore.add(plugin.messages().raw("menu.lore-best", Map.of(
                        "best", best == null ? "--" : ActionBarService.formatDuration(best))));
            }
            inventory.setItem(slot, icon(material, plugin.messages().raw(nameKey, vars),
                    lore.toArray(new String[0])));
            if (selectable) {
                selectableSlots.put(slot, level);
            }
            slot++;
            if (slot >= 45) {
                break;
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        Zone level = selectableSlots.get(slot);
        if (level == null) {
            plugin.messages().send(viewer, "command.invalid-hierarchy", Map.of("reason", "该关卡未解锁"));
            return;
        }
        viewer.closeInventory();
        selectLevel(level);
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
        plugin.scheduler().runEntity(viewer, p -> p.teleport(startLocation(start)), () -> {});
    }

    private Zone findStart(Zone level) {
        for (Zone c : plugin.zoneRepository().tree().childrenOf(level.id())) {
            if (c.type() == ZoneType.START) {
                return c;
            }
        }
        return null;
    }

    private Location startLocation(Zone start) {
        if (start.shape() == SelectionShape.CUBOID) {
            double cx = (start.minX() + start.maxX()) / 2.0 + 0.5;
            double cz = (start.minZ() + start.maxZ()) / 2.0 + 0.5;
            return new Location(viewer.getWorld(), cx, start.maxY() + 1, cz);
        }
        return new Location(viewer.getWorld(), start.centerX(), start.centerY(), start.centerZ());
    }
}
