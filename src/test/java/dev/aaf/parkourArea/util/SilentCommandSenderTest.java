package dev.aaf.parkourArea.util;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SilentCommandSenderTest {

    /** 记录调用的假 delegate：sendMessage 计数、权限查询透传。 */
    static final class FakeSender implements CommandSender {
        int messageCalls = 0;
        int permissionCalls = 0;

        @Override public void sendMessage(String message) { messageCalls++; }
        @Override public void sendMessage(String... messages) { messageCalls++; }
        @Override public void sendMessage(UUID sender, String message) { messageCalls++; }
        @Override public void sendMessage(UUID sender, String... messages) { messageCalls++; }
        @Override public boolean isPermissionSet(String name) { return true; }
        @Override public boolean isPermissionSet(Permission perm) { return true; }
        @Override public boolean hasPermission(String name) { permissionCalls++; return true; }
        @Override public boolean hasPermission(Permission perm) { permissionCalls++; return true; }
        @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return null; }
        @Override public PermissionAttachment addAttachment(Plugin plugin) { return null; }
        @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return null; }
        @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return null; }
        @Override public void removeAttachment(PermissionAttachment attachment) {}
        @Override public void recalculatePermissions() {}
        @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return Set.of(); }
        @Override public boolean isOp() { return true; }
        @Override public void setOp(boolean value) {}
        @Override public String getName() { return "Fake"; }
        @Override public Component name() { return Component.text("Fake"); }
        @Override public Server getServer() { return null; }
        @Override public Spigot spigot() { return null; }
    }

    private final FakeSender delegate = new FakeSender();
    private final SilentCommandSender silent = SilentCommandSender.wrapping(delegate);

    @Test
    void swallowsAllLegacyMessageOverloads() {
        silent.sendMessage("a");
        silent.sendMessage("a", "b");
        silent.sendMessage(UUID.randomUUID(), "a");
        silent.sendMessage(UUID.randomUUID(), "a", "b");
        assertThat(delegate.messageCalls).isZero();
    }

    @Test
    void swallowsAdventureMessage() {
        assertThatCode(() -> silent.sendMessage(
                Identity.nil(), Component.text("hi"), MessageType.SYSTEM))
                .doesNotThrowAnyException();
        assertThat(delegate.messageCalls).isZero();
    }

    @Test
    void delegatesIdentityAndPermissions() {
        assertThat(silent.hasPermission("any.node")).isTrue();
        assertThat(silent.isOp()).isTrue();
        assertThat(silent.getName()).isEqualTo("Fake");
        assertThat(delegate.permissionCalls).isEqualTo(1);
    }
}
