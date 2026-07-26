package dev.aaf.parkourArea.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColorUtilTest {

    @Test
    void legacyCodesWithAngleBracketsStayLegacy() {
        // 实机反馈：用法文本含 <type> 被误判为 MiniMessage，& 码原样输出
        Component c = ColorUtil.parse("&8[&bParkour&8] &r&c参数不足。用法: <type> <name|->");
        String plain = PlainTextComponentSerializer.plainText().serialize(c);
        assertThat(plain).isEqualTo("[Parkour] 参数不足。用法: <type> <name|->");
        String legacy = LegacyComponentSerializer.legacySection().serialize(c);
        assertThat(legacy).contains("§c");
        assertThat(legacy).doesNotContain("&");
    }

    @Test
    void pureMiniMessageStillParsedAsMiniMessage() {
        Component c = ColorUtil.parse("<green>hi</green>");
        assertThat(PlainTextComponentSerializer.plainText().serialize(c)).isEqualTo("hi");
        assertThat(LegacyComponentSerializer.legacySection().serialize(c)).contains("§a");
    }

    @Test
    void varsSubstitutedBeforeColorParse() {
        Component c = ColorUtil.parse("&c区域层级非法: {reason}",
                java.util.Map.of("reason", "用法: <type> <name|->"));
        assertThat(PlainTextComponentSerializer.plainText().serialize(c))
                .isEqualTo("区域层级非法: 用法: <type> <name|->");
    }

    @Test
    void plainTextPassesThrough() {
        Component c = ColorUtil.parse("无颜色文本");
        assertThat(PlainTextComponentSerializer.plainText().serialize(c)).isEqualTo("无颜色文本");
    }

    @Test
    void newlinesBecomeRealLineBreaks() {
        // 实机反馈：actionbar.playing 的 \n 被渲染成可见的 LF 占位符而非真正换行
        Component c = ColorUtil.parse("&b当前 &r{stars}\n&7best: &a{time}");
        String plain = PlainTextComponentSerializer.plainText().serialize(c);
        // 应含真实换行，且两行分别为对应文本
        assertThat(plain).contains("\n");
        assertThat(plain.split("\n", -1)[0]).contains("当前");
        assertThat(plain.split("\n", -1)[1]).contains("best");
        // 颜色在两段都生效（& 码转 §，无裸 &）
        String legacy = LegacyComponentSerializer.legacySection().serialize(c);
        assertThat(legacy).contains("§");
        assertThat(legacy).doesNotContain("&b").doesNotContain("&7");
    }

    @Test
    void multilineWithVarsPreserved() {
        Component c = ColorUtil.parse("&a{a}\n&b{b}",
                java.util.Map.of("a", "第一行", "b", "第二行"));
        String[] lines = PlainTextComponentSerializer.plainText().serialize(c).split("\n", -1);
        assertThat(lines[0]).isEqualTo("第一行");
        assertThat(lines[1]).isEqualTo("第二行");
    }
}
