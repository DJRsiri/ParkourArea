package dev.aaf.parkourArea.blocks;

import java.util.List;

/** 单个方块的踩踏触发配置（command/sound/repeat/interval）。command 或 sound 为空列表表示禁用。 */
public record BlockCommandConfig(List<String> command, List<String> sound, boolean repeat, int interval) {
}
