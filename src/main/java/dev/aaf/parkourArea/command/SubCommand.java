package dev.aaf.parkourArea.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/** 子命令接口，由各功能模块实现并注册到 {@link ParkourCommand}。 */
public interface SubCommand {

    /** 子命令名（如 {@code create}）。 */
    String name();

    /** 别名（可为空数组）。 */
    default String[] aliases() {
        return new String[0];
    }

    /** 所需权限节点（null 表示仅需基础 user 权限）。 */
    String permission();

    /** 帮助描述。 */
    String description();

    /** 用法（如 {@code <zonetype> [zonename] [pos1] [pos2]}）。 */
    default String usage() {
        return "";
    }

    /** 执行。args 为去掉子命令名后的参数。 */
    void execute(CommandSender sender, String[] args);

    /** Tab 补全。args 为去掉子命令名后的参数。 */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
