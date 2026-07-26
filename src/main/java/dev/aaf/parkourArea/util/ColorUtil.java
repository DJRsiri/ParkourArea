package dev.aaf.parkourArea.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 颜色文本解析：同时兼容 legacy {@code &a} 码与 MiniMessage {@code <green>} 标签。
 *
 * <p>解析优先级：<b>含 legacy {@code &} 码时一律走 legacy</b>（MiniMessage 不解析 & 码会原样输出，
 * 而 legacy 会把 {@code <xxx>} 当纯文本，混排时 legacy 不会丢颜色）；
 * 仅当不含 & 码且检测到 MiniMessage 标签特征（{@code <} 后跟字母/#/:/!）时走 MiniMessage。</p>
 */
public final class ColorUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** MiniMessage 标签特征：<后紧跟字母、#、:、/、!（如 <green> <#ff0000> <lang:> </...> <!italic>）。 */
    private static final Pattern MINI_TAG = Pattern.compile("<[a-zA-Z#:/!]");
    /** legacy 码特征：& + 颜色/格式字符，或 &#RRGGBB。 */
    private static final Pattern LEGACY_CODE = Pattern.compile("&(#[0-9a-fA-F]{6}|[0-9a-fA-Fk-oK-OrRxX])");

    private ColorUtil() {}

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        // 文本中的 LF（如 messages.yml 双引号 "\n" 经 YAML 转义而来）切分成真正的换行 Component，
        // 否则 sendActionBar 等会把裸 LF 渲染成可见的换行占位符。每段独立判定 legacy/MiniMessage。
        String[] parts = text.split("\n", -1);
        Component result = parseSingle(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = result.append(Component.newline()).append(parseSingle(parts[i]));
        }
        return result;
    }

    private static Component parseSingle(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (LEGACY_CODE.matcher(text).find()) {
            return LEGACY.deserialize(text);
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
