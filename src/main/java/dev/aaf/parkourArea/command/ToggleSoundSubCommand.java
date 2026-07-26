package dev.aaf.parkourArea.command;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.player.ParkourPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /parkour togglesound — 切换所有跑酷音效总开关（与音效工具联动）。 */
public final class ToggleSoundSubCommand implements SubCommand {

    private final ParkourArea plugin;

    public ToggleSoundSubCommand(ParkourArea plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "togglesound";
    }

    @Override
    public String permission() {
        return Permission.USER;
    }

    @Override
    public String description() {
        return "切换所有跑酷音效总开关";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return;
        }
        ParkourPlayer session = plugin.sessionService().get(player.getUniqueId());
        if (session == null) {
            return;
        }
        boolean next = !session.soundEnabled();
        session.soundEnabled(next);
        plugin.messages().send(sender, next ? "command.sound-master-on" : "command.sound-master-off");
        if (plugin.preferenceService() != null) {
            plugin.preferenceService().saveAsync(player.getUniqueId(), session);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
