package dev.aaf.parkourArea.editmode;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 编辑模式开关（玩家级）。
 *
 * <p>需求：未进入编辑模式时无法放/破坏区域方块、无法用编辑命令；进入后可自由编辑但不触发关卡交互。
 * 默认状态由 config {@code edit-mode-default} 控制（false 时需显式开启）。</p>
 */
public final class EditModeService {

    private final Set<UUID> editMode = new HashSet<>();
    private final boolean defaultOn;

    public EditModeService(boolean defaultOn) {
        this.defaultOn = defaultOn;
    }

    public boolean isEditMode(UUID uuid) {
        return editMode.contains(uuid) || defaultOn;
    }

    public boolean isEditMode(Player player) {
        return isEditMode(player.getUniqueId());
    }

    public void set(UUID uuid, boolean on) {
        if (on) {
            editMode.add(uuid);
        } else {
            editMode.remove(uuid);
        }
    }

    public boolean toggle(UUID uuid) {
        if (editMode.remove(uuid)) {
            return false;
        }
        editMode.add(uuid);
        return true;
    }
}
