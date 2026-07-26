package dev.aaf.parkourArea.hooks.papi;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.persistence.ProgressStatus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI 变量：
 * <ul>
 *   <li>%parkour_best_&lt;levelId&gt;% — 该关最佳用时</li>
 *   <li>%parkour_status_&lt;levelId&gt;% — 该关进度状态（NONE/VISITED/COMPLETED）</li>
 *   <li>%parkour_completed_count% — 已通关关卡数</li>
 * </ul>
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final ParkourArea plugin;

    public PlaceholderHook(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "parkour";
    }

    @Override
    public String getAuthor() {
        return "AethelArcticFox";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }
        if (params.startsWith("best_")) {
            int levelId = parseInt(params.substring(5));
            if (levelId < 0) {
                return "";
            }
            Long best = plugin.timerService().getBest(player.getUniqueId(), levelId);
            return best == null ? "--" : ActionBarService.formatDuration(best);
        }
        if (params.startsWith("status_")) {
            int levelId = parseInt(params.substring(7));
            if (levelId < 0) {
                return "";
            }
            return plugin.progressService().getStatus(player.getUniqueId(), levelId).name();
        }
        if (params.equals("completed_count")) {
            int count = 0;
            for (ProgressStatus s : plugin.progressService().getStatuses(player.getUniqueId()).values()) {
                if (s == ProgressStatus.COMPLETED) {
                    count++;
                }
            }
            return String.valueOf(count);
        }
        return "";
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
