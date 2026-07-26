package dev.aaf.parkourArea.hotbar;

import dev.aaf.parkourArea.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** 快捷工具栏物品工厂，用 PersistentDataContainer 标记 action 以便点击识别。 */
public final class HotbarItems {

    public static final String ACTION_REPLAY = "replay";
    public static final String ACTION_LOBBY = "lobby";
    public static final String ACTION_CHECKPOINT = "checkpoint";
    public static final String ACTION_MENU = "menu";
    public static final String ACTION_SOUND = "sound";
    public static final String ACTION_VISIBILITY = "visibility";

    private final NamespacedKey actionKey;

    public HotbarItems(NamespacedKey actionKey) {
        this.actionKey = actionKey;
    }

    public ItemStack replay() {
        return tool(Material.PAPER, "&b重玩本关", ACTION_REPLAY);
    }

    public ItemStack lobby() {
        return tool(Material.RED_BED, "&e返回大厅", ACTION_LOBBY);
    }

    public ItemStack checkpoint() {
        return tool(Material.COMPASS, "&a回到存档点", ACTION_CHECKPOINT);
    }

    public ItemStack menu() {
        return tool(Material.NETHER_STAR, "&6菜单", ACTION_MENU);
    }

    public ItemStack sound() {
        return tool(Material.NOTE_BLOCK, "&a音效切换", ACTION_SOUND);
    }

    public ItemStack visibility() {
        return tool(Material.SPYGLASS, "&b玩家可见性", ACTION_VISIBILITY);
    }

    private ItemStack tool(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(name));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** 取物品的 action 标记；非工具返回 null。 */
    public String actionOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public boolean isHotbarTool(ItemStack item) {
        return actionOf(item) != null;
    }
}
