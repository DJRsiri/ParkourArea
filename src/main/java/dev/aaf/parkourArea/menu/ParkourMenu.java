package dev.aaf.parkourArea.menu;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** 菜单基类：绑定查看玩家，自定义 InventoryHolder，由 MenuListener 路由点击。 */
public abstract class ParkourMenu implements InventoryHolder {

    protected final ParkourArea plugin;
    protected final Player viewer;
    protected final Inventory inventory;

    public ParkourMenu(ParkourArea plugin, Player viewer, String title, int size) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, size, ColorUtil.parse(title));
    }

    public abstract void onClick(InventoryClickEvent event);

    public abstract void render();

    public void open() {
        render();
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(name));
            if (lore.length > 0) {
                List<Component> loreComp = new ArrayList<>();
                for (String l : lore) {
                    loreComp.add(ColorUtil.parse(l));
                }
                meta.lore(loreComp);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
