package dev.aaf.parkourArea.util;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * 静默命令发送者：以控制台的身份与权限执行命令，但吞掉全部命令反馈
 * （成功/失败消息），避免 blocks.yml 等高频触发命令刷屏控制台与管理员客户端。
 *
 * <p>原版命令反馈只发给执行者本身（玩家执行者才会按 sendCommandFeedback
 * 广播给管理员），这里把执行者替换为本类即可让消息到此为止——
 * 无需也不应改动 gamerule，不影响服务器原版规则。</p>
 */
public final class SilentCommandSender implements CommandSender {

    private static SilentCommandSender consoleBacked;

    private final CommandSender delegate;

    private SilentCommandSender(CommandSender delegate) {
        this.delegate = delegate;
    }

    /** 以控制台为 delegate 的单例（延迟初始化，避免类加载期触碰 Bukkit 静态）。 */
    public static synchronized SilentCommandSender console() {
        if (consoleBacked == null) {
            consoleBacked = new SilentCommandSender(Bukkit.getConsoleSender());
        }
        return consoleBacked;
    }

    /** 测试用：包装指定 delegate。 */
    static SilentCommandSender wrapping(CommandSender delegate) {
        return new SilentCommandSender(delegate);
    }

    // ---- 吞掉全部反馈 ----

    @Override
    public void sendMessage(@NotNull String message) {
        // 静默
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        // 静默
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        // 静默
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        // 静默
    }

    /** Paper 的 default 桥接会把 adventure 消息转回 legacy，这里直接截断更稳妥。 */
    @Override
    public void sendMessage(@NotNull Identity identity, @NotNull Component message,
                            @NotNull MessageType type) {
        // 静默
    }

    // ---- 身份与权限委托 console（命令权限检查照常通过） ----

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return delegate.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return delegate.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return delegate.hasPermission(name);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return delegate.hasPermission(perm);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name,
                                                       boolean value) {
        return delegate.addAttachment(plugin, name, value);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return delegate.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name,
                                              boolean value, int ticks) {
        return delegate.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return delegate.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        delegate.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        delegate.recalculatePermissions();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return delegate.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return delegate.isOp();
    }

    @Override
    public void setOp(boolean value) {
        delegate.setOp(value);
    }

    @Override
    public @NotNull Component name() {
        return delegate.name();
    }

    @Override
    public @NotNull String getName() {
        return delegate.getName();
    }

    /** 原版命令反馈不走 spigot 通道，委托即可。 */
    @Override
    public @NotNull Spigot spigot() {
        return delegate.spigot();
    }

    @Override
    public @NotNull Server getServer() {
        return delegate.getServer();
    }
}
