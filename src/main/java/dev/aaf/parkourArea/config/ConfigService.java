package dev.aaf.parkourArea.config;

import dev.aaf.parkourArea.player.VisibilityMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集中加载 config.yml / messages.yml / blocks.yml / ratings.yml。
 * zones.yml 由 {@code zone.ZoneRepository} 单独管理（带原子写盘）。
 */
public final class ConfigService {

    private final JavaPlugin plugin;
    private Settings settings;
    private final Messages messages = new Messages();
    private FileConfiguration blocksCfg;
    private FileConfiguration ratingsCfg;
    private ConfigVersionChecker versionChecker;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // ---- config.yml ----
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        Set<GameMode> modes = new HashSet<>();
        List<String> modeNames = cfg.getStringList("settings.require-gamemode");
        if (modeNames.isEmpty()) {
            modeNames = List.of("SURVIVAL", "ADVENTURE");
        }
        for (String s : modeNames) {
            try {
                modes.add(GameMode.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("未知的游戏模式: " + s);
            }
        }

        Material cpBlock;
        try {
            cpBlock = Material.valueOf(
                    cfg.getString("settings.checkpoint-block", "GOLD_BLOCK").toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("未知的存档点方块，回退为 GOLD_BLOCK");
            cpBlock = Material.GOLD_BLOCK;
        }

        this.settings = new Settings(
                cfg.getInt("settings.detect-interval-ticks", 10),
                cfg.getInt("settings.actionbar-interval-ticks", 5),
                cfg.getInt("settings.anti-idle-seconds", 60),
                cfg.getDouble("settings.anti-idle-rotation-threshold-deg", 5.0),
                cfg.getInt("settings.top-record-count", 10),
                modes,
                cpBlock,
                cfg.getString("settings.checkpoint-success-sound", "entity.player.levelup"),
                cfg.getBoolean("settings.checkpoint-sound-enabled", true),
                cfg.getLong("settings.rating-flicker-interval-millis", 500),
                cfg.getBoolean("edit-mode-default", false),
                cfg.getBoolean("debug", false),
                cfg.getBoolean("settings.sound-enabled", true),
                cfg.getBoolean("settings.block-sound-enabled", true),
                VisibilityMode.parse(cfg.getString("settings.default-visibility-mode", "FULL"),
                        VisibilityMode.FULL),
                cfg.getBoolean("settings.teleport-keep-rotation", true),
                cfg.getBoolean("settings.skip-detection", true),
                cfg.getBoolean("settings.allow-any-selectable", false)
        );

        // ---- messages.yml ----
        File msgFile = ensure("messages.yml");
        messages.load(msgFile);

        // ---- blocks.yml ----
        blocksCfg = YamlConfiguration.loadConfiguration(ensure("blocks.yml"));

        // ---- ratings.yml ----
        ratingsCfg = YamlConfiguration.loadConfiguration(ensure("ratings.yml"));

        // ---- 配置文件版本检测（控制台 + 游戏内管理员警告）----
        if (versionChecker == null) {
            versionChecker = new ConfigVersionChecker(plugin);
        }
        List<ConfigVersionChecker.Outdated> outdated = versionChecker.check();
        for (ConfigVersionChecker.Outdated o : outdated) {
            plugin.getLogger().warning("配置文件 " + o.file() + " 版本过旧 (当前 v" + o.current()
                    + ", 期望 v" + o.expected() + ")，请用 /parkour updateconf 更新"
                    + "或 /parkour recreateconf 重建");
            messages.sendToAdmins("command.config-outdated", Map.of(
                    "file", o.file(),
                    "current", String.valueOf(o.current()),
                    "expected", String.valueOf(o.expected())));
        }
    }

    private File ensure(String resource) {
        File f = new File(plugin.getDataFolder(), resource);
        if (!f.exists()) {
            plugin.saveResource(resource, false);
        }
        return f;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public FileConfiguration blocks() {
        return blocksCfg;
    }

    public FileConfiguration ratings() {
        return ratingsCfg;
    }

    public ConfigVersionChecker versionChecker() {
        return versionChecker;
    }
}
