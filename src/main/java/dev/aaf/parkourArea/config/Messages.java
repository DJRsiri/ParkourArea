package dev.aaf.parkourArea.config;

import dev.aaf.parkourArea.command.Permission;
import dev.aaf.parkourArea.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

/**
 * messages.yml 加载器与文案解析。
 *
 * <p>{@link #prefixed} / {@link #send} 用于聊天回复（带 prefix）；
 * {@link #plain} 用于 actionbar、菜单标题、物品名等（不带 prefix）。
 * 颜色同时兼容 legacy {@code &} 与 MiniMessage。</p>
 */
public final class Messages {

    private FileConfiguration cfg;
    private String prefix = "";

    public void load(File file) {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        this.prefix = cfg.getString("prefix", "");
    }

    public String raw(String path) {
        return cfg.getString(path, path);
    }

    /** 不带 prefix、不解析颜色（用于变量替换后的二次处理或纯文本场景）。 */
    public String raw(String path, Map<String, String> vars) {
        return ColorUtil.replaceVars(cfg.getString(path, path), vars);
    }

    /** 不带 prefix，解析颜色 + 变量（用于 actionbar、菜单、物品名）。 */
    public Component plain(String path) {
        return ColorUtil.parse(cfg.getString(path, path));
    }

    public Component plain(String path, Map<String, String> vars) {
        return ColorUtil.parse(cfg.getString(path, path), vars);
    }

    /** 带 prefix，解析颜色 + 变量（用于聊天命令回复）。 */
    public Component prefixed(String path, Map<String, String> vars) {
        String text = prefix + cfg.getString(path, path);
        return ColorUtil.parse(text, vars);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(prefixed(path, null));
    }

    public void send(CommandSender sender, String path, Map<String, String> vars) {
        sender.sendMessage(prefixed(path, vars));
    }

    /** 向所有在线管理员（parkour.admin）发送（版本警告等管理员通知）。 */
    public void sendToAdmins(String path, Map<String, String> vars) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(Permission.ADMIN)) {
                send(p, path, vars);
            }
        }
    }

    public Component prefix() {
        return ColorUtil.parse(prefix);
    }

    public FileConfiguration raw() {
        return cfg;
    }
}
