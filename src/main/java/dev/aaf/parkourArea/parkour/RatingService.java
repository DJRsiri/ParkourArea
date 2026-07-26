package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.ParkourArea;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 用时评级（降星）服务。
 *
 * <p>每关在 ratings.yml 中以关卡区域 ID 为 key 配置 3 档时间阈值：
 * <ul>
 *   <li>≤ tier1：&a⭐⭐⭐（绿）</li>
 *   <li>≤ tier2：&e⭐⭐&7⭐（黄）</li>
 *   <li>≤ tier3：&c⭐&7⭐⭐（红）</li>
 *   <li>＞ tier3：每 {@code rating-flicker-interval-millis} 在 &7⭐⭐⭐ 与 &8⭐⭐⭐ 间交替闪烁</li>
 * </ul>
 * 无评级配置则返回空串（不显示星级）。</p>
 */
public final class RatingService {

    private final ParkourArea plugin;

    public RatingService(ParkourArea plugin) {
        this.plugin = plugin;
    }

    /** 渲染星级字符串（legacy & 码）。无配置返回空串。 */
    public String renderStars(int levelId, long durationMillis, long currentMillis) {
        FileConfiguration ratings = plugin.configService().ratings();
        String path = "ratings." + levelId;
        if (!ratings.contains(path)) {
            return "";
        }
        long t1 = ratings.getLong(path + ".tier1-millis", Long.MAX_VALUE);
        long t2 = ratings.getLong(path + ".tier2-millis", Long.MAX_VALUE);
        long t3 = ratings.getLong(path + ".tier3-millis", Long.MAX_VALUE);
        if (durationMillis <= t1) {
            return "&a⭐⭐⭐";
        }
        if (durationMillis <= t2) {
            return "&e⭐⭐&7⭐";
        }
        if (durationMillis <= t3) {
            return "&c⭐&7⭐⭐";
        }
        long flickerInterval = plugin.configService().settings().ratingFlickerIntervalMillis();
        boolean on = (currentMillis / Math.max(1, flickerInterval)) % 2 == 0;
        return on ? "&7⭐⭐⭐" : "&8⭐⭐⭐";
    }

    /** 该关卡是否配置了评级。 */
    public boolean hasRating(int levelId) {
        return plugin.configService().ratings().contains("ratings." + levelId);
    }
}
