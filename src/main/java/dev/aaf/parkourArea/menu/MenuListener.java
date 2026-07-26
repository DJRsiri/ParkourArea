package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.hotbar.HotbarItems;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.zone.SelectionShape;
import dev.aaf.parkourArea.zone.Zone;
import dev.aaf.parkourArea.zone.ZoneType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 监听菜单点击（路由到 ParkourMenu）与快捷工具栏点击（replay/lobby/checkpoint/menu）。
 */
public final class MenuListener implements Listener {

    private final ParkourArea plugin;

    public MenuListener(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ParkourMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getInventory())
                && event.getWhoClicked() instanceof Player) {
            menu.onClick(event);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        String action = plugin.hotbarService().items().actionOf(item);
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        switch (action) {
            case HotbarItems.ACTION_MENU -> new MainMenu(plugin, player).open();
            case HotbarItems.ACTION_LOBBY -> plugin.sessionService().returnToLobby(player);
            case HotbarItems.ACTION_REPLAY -> replayLevel(player);
            case HotbarItems.ACTION_CHECKPOINT -> backToCheckpoint(player);
            case HotbarItems.ACTION_SOUND -> plugin.preferenceService().cycleMasterSound(player);
            case HotbarItems.ACTION_VISIBILITY -> plugin.visibilityService().cycleMode(player);
            default -> { }
        }
    }

    private void replayLevel(Player player) {
        ParkourPlayer session = plugin.sessionService().get(player.getUniqueId());
        if (session == null || session.selectedLevel() == null) {
            return;
        }
        Zone level = session.selectedLevel();
        Zone start = findStart(level);
        if (start == null) {
            return;
        }
        session.phase(PlayerPhase.AT_START);
        session.clearCheckpoint();
        plugin.scheduler().runEntity(player, p -> {
            org.bukkit.World world = start.worldUid() != null
                    ? org.bukkit.Bukkit.getWorld(start.worldUid()) : p.getWorld();
            if (world != null) {
                // 坐标取自 START（spawn 或中心 + 最高非空气方块），朝向取自所属 LEVEL 的 spawn（缺省 0/0）
                p.teleport(dev.aaf.parkourArea.util.Locations.teleportLocation(world, start, level));
            }
        }, () -> {});
        plugin.messages().send(player, "parkour.teleported-start");
    }

    /** 回到最后一个记录到的中途存档点。 */
    private void backToCheckpoint(Player player) {
        ParkourPlayer session = plugin.sessionService().get(player.getUniqueId());
        if (session == null || !session.hasCheckpoint()) {
            plugin.messages().send(player, "command.not-found", java.util.Map.of("query", "存档点"));
            return;
        }
        int x = session.checkpointX();
        int y = session.checkpointY();
        int z = session.checkpointZ();
        plugin.scheduler().runEntity(player, p -> {
            p.teleport(new Location(p.getWorld(), x + 0.5, y + 1, z + 0.5));
            plugin.messages().send(p, "parkour.teleported-checkpoint");
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
