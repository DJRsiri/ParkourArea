package dev.aaf.parkourArea.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 音效播放工具。
 *
 * <p>使用 {@link Player#playSound(Location, String, float, float)} 的 String 重载，
 * 直接接受音效命名空间字符串（如 {@code entity.player.levelup} / {@code minecraft:block.lava.pop}），
 * 兼容自定义音效资源包。调用方需确保已在玩家所属线程上执行。</p>
 */
public final class SoundPlayer {

    public static final float DEFAULT_VOLUME = 1.0f;
    public static final float DEFAULT_PITCH = 1.0f;

    private SoundPlayer() {}

    public static void play(Player player, String soundName) {
        play(player, soundName, DEFAULT_VOLUME, DEFAULT_PITCH);
    }

    public static void play(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isEmpty()) {
            return;
        }
        player.playSound(player.getLocation(), soundName, volume, pitch);
    }
}
