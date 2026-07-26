package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /parkour editmode [true/false] — 切换编辑模式（无参数则切换）。 */
public final class EditModeSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public EditModeSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "editmode";
    }

    @Override
    public String permission() {
        return Permission.EDIT;
    }

    @Override
    public String description() {
        return "切换编辑模式";
    }

    @Override
    public String usage() {
        return "[true|false]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return;
        }
        boolean next;
        if (args.length >= 1) {
            if ("true".equalsIgnoreCase(args[0]) || "on".equalsIgnoreCase(args[0])) {
                next = true;
            } else if ("false".equalsIgnoreCase(args[0]) || "off".equalsIgnoreCase(args[0])) {
                next = false;
            } else {
                plugin.messages().send(sender, "command.invalid-hierarchy",
                        java.util.Map.of("reason", "参数应为 true/false"));
                return;
            }
        } else {
            next = !plugin.editModeService().isEditMode(player);
        }
        plugin.editModeService().set(player.getUniqueId(), next);
        plugin.messages().send(sender, next ? "command.edit-mode-on" : "command.edit-mode-off");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return java.util.stream.Stream.of("true", "false")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
