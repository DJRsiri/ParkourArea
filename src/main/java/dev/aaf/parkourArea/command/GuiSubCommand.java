package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.menu.MainMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /parkour gui — 打开主菜单。 */
public final class GuiSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public GuiSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return "打开菜单";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return;
        }
        new MainMenu(plugin, player).open();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
