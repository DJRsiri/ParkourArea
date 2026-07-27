package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.config.ConfigVersionChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /parkour recreateconf — 二次确认后，把 config/messages/blocks/ratings.yml
 * 重命名为 .bak 备份并从 jar 重建（不含 zones.yml 与 parkour.db）。控制台可用。
 */
public final class RecreateConfSubCommand implements SubCommand {

    /** 控制台在 ConfirmFlow 中的固定标识。 */
    private static final UUID CONSOLE_ID = new UUID(0L, 0L);

    private final ParkourArea plugin;

    public RecreateConfSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "recreateconf";
    }

    @Override
    public String permission() {
        return Permission.ADMIN;
    }

    @Override
    public String description() {
        return "重建全部配置文件（原文件备份为 .bak，二次确认）";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        UUID confirmId = sender instanceof Player p ? p.getUniqueId() : CONSOLE_ID;
        if (plugin.confirmFlow().hasPending(confirmId)) {
            plugin.confirmFlow().confirm(confirmId);
            return;
        }
        plugin.messages().send(sender, "command.confirm-required",
                Map.of("command", "parkour recreateconf"));
        plugin.confirmFlow().request(confirmId, confirmed -> {
            if (!confirmed) {
                plugin.messages().send(sender, "command.cancelled");
                return;
            }
            ConfigVersionChecker checker = plugin.configService().versionChecker();
            List<String> recreated = new ArrayList<>();
            for (String name : ConfigVersionChecker.VERSION_KEYS.keySet()) {
                File f = checker.fileOf(name);
                try {
                    if (f.exists()) {
                        Files.move(f.toPath(),
                                ConfigVersionChecker.nextBackupFile(f).toPath());
                    }
                    checker.saveResource(name);
                    recreated.add(name);
                } catch (IOException | IllegalArgumentException e) {
                    plugin.getLogger().warning("重建 " + name + " 失败: " + e.getMessage());
                }
            }
            plugin.reloadAll();
            plugin.messages().send(sender, "command.config-recreated",
                    Map.of("files", String.join(", ", recreated)));
        });
    }
}
