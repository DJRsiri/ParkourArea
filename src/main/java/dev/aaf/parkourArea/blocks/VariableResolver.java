package dev.aaf.parkourArea.blocks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

/**
 * 变量替换：%player% 等内置变量 + PlaceholderAPI（软依赖，反射调用避免硬依赖）。
 */
public final class VariableResolver {

    private static Boolean papiEnabled;
    private static Method setPlaceholdersMethod;

    private VariableResolver() {}

    public static String resolve(String text, OfflinePlayer player) {
        if (text == null) {
            return "";
        }
        String result = text.replace("%player%", player == null ? "" : player.getName());
        if (result.indexOf('%') >= 0 && isPapiEnabled()) {
            result = applyPapi(player, result);
        }
        return result;
    }

    private static boolean isPapiEnabled() {
        if (papiEnabled == null) {
            papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
            if (papiEnabled) {
                try {
                    Class<?> cls = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    setPlaceholdersMethod = cls.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                } catch (Throwable t) {
                    papiEnabled = false;
                }
            }
        }
        return papiEnabled;
    }

    private static String applyPapi(OfflinePlayer player, String text) {
        if (setPlaceholdersMethod == null) {
            return text;
        }
        try {
            Object out = setPlaceholdersMethod.invoke(null, player, text);
            return out == null ? text : out.toString();
        } catch (Throwable t) {
            return text;
        }
    }
}
