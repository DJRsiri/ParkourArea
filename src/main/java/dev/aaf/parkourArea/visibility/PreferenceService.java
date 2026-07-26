package dev.aaf.parkourArea.visibility;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.persistence.Preference;
import dev.aaf.parkourArea.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** 偏好异步落库与切换服务（音效三层 + 可见性挡位）。best-effort，失败仅告警。 */
public final class PreferenceService {

    private final ParkourArea plugin;

    public PreferenceService(ParkourArea plugin) {
        this.plugin = plugin;
    }

    public void saveAsync(UUID uuid, ParkourPlayer session) {
        if (session == null || plugin.preferenceDao() == null) {
            return;
        }
        Preference pref = new Preference(session.soundEnabled(), session.checkpointSoundEnabled(),
                session.blockSoundEnabled(), session.visibilityMode());
        plugin.scheduler().runAsync(() -> {
            try {
                plugin.preferenceDao().upsert(uuid, pref);
            } catch (Exception e) {
                plugin.getLogger().warning("保存玩家偏好失败: " + e.getMessage());
            }
        });
    }

    /** 工具右键：循环切换所有跑酷音效总开关。调用方可能不在 global 线程，内部切回。 */
    public void cycleMasterSound(Player player) {
        UUID uid = player.getUniqueId();
        plugin.scheduler().runGlobal(() -> {
            ParkourPlayer s = plugin.sessionService().get(uid);
            if (s == null) {
                return;
            }
            boolean next = !s.soundEnabled();
            s.soundEnabled(next);
            saveAsync(uid, s);
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                plugin.messages().send(p, next ? "command.sound-master-on" : "command.sound-master-off");
            }
        });
    }
}
