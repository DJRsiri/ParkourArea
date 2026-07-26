package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/** /parkour reload — 重载全部配置（委托主类 reloadAll，避免子命令耦合具体组件）。 */
public final class ReloadSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public ReloadSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return Permission.ADMIN;
    }

    @Override
    public String description() {
        return "重载配置文件";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadAll();
        plugin.messages().send(sender, "command.reloaded");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
