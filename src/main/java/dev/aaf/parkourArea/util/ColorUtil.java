package dev.aaf.parkourArea.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 颜色文本解析：同时兼容 legacy {@code &a} 码与 MiniMessage {@code <green>} 标签。
 *
 * <p>用户配置习惯用 {@code &a⭐⭐⭐}，故默认走 legacy；当文本检测到 MiniMessage 标签特征时
 * （{@code <} 后跟字母/#/:/!），改用 MiniMessage 解析。</p>
 */
public final class ColorUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** MiniMessage 标签特征：<后紧跟字母、#、:、/、!（如 <green> <#ff0000> <lang:> </...> <!italic>）。 */
    private static final Pattern MINI_TAG = Pattern.compile("<[a-zA-Z#:/!]");

    private ColorUtil() {}

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (MINI_TAG.matcher(text).find()) {
            return MINI.deserialize(text);
        }
        return LEGACY.deserialize(text);
    }

    /** 先替换 {key} 占位变量，再解析颜色。 */
    public static Component parse(String text, Map<String, String> vars) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String result = text;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                result = result.replace("{" + e.getKey() + "}",
                        e.getValue() == null ? "" : e.getValue());
            }
        }
        return parse(result);
    }

    /** 仅做 {key} 占位替换，不解析颜色（用于命令执行等场景）。 */
    public static String replaceVars(String text, Map<String, String> vars) {
        if (text == null) {
            return "";
        }
        String result = text;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                result = result.replace("{" + e.getKey() + "}",
                        e.getValue() == null ? "" : e.getValue());
            }
        }
        return result;
    }
}
