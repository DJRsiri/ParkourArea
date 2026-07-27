package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.config.ConfigVersionChecker;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /parkour updateconf — 备份并 defaults 合并更新过旧配置文件（控制台可用）。 */
public final class UpdateConfSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public UpdateConfSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "updateconf";
    }

    @Override
    public String permission() {
        return Permission.ADMIN;
    }

    @Override
    public String description() {
        return "更新过旧配置文件（备份为 .bak 并合并新增配置项）";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ConfigVersionChecker checker = plugin.configService().versionChecker();
        List<ConfigVersionChecker.Outdated> outdated = checker.check();
        if (outdated.isEmpty()) {
            plugin.messages().send(sender, "command.config-up-to-date");
            return;
        }
        List<String> updated = new ArrayList<>();
        for (ConfigVersionChecker.Outdated o : outdated) {
            File target = checker.fileOf(o.file());
            // 1) 备份
            try {
                if (target.exists()) {
                    Files.copy(target.toPath(),
                            ConfigVersionChecker.nextBackupFile(target).toPath());
                }
            } catch (IOException e) {
                plugin.messages().send(sender, "command.config-update-failed",
                        Map.of("file", o.file()));
                continue;
            }
            // 2) defaults 合并（false=旧文件损坏，提示重建）
            boolean ok;
            try {
                ok = checker.mergeWithDefaults(o.file());
            } catch (IOException e) {
                plugin.getLogger().warning("合并 " + o.file() + " 失败: " + e.getMessage());
                ok = false;
            }
            if (!ok) {
                plugin.messages().send(sender, "command.config-update-failed",
                        Map.of("file", o.file()));
                continue;
            }
            updated.add(o.file());
        }
        plugin.reloadAll();
        if (!updated.isEmpty()) {
            plugin.messages().send(sender, "command.config-updated",
                    Map.of("files", String.join(", ", updated)));
        }
    }
}
