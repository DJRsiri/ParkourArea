package dev.aaf.parkourArea.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedAttribute;
import com.comphenix.protocol.wrappers.WrappedAttributeModifier;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 构造并给单个观察者发送 UPDATE_ATTRIBUTES 包，对目标实体注入 generic.scale 属性修饰符。
 *
 * <p>1.21 下 generic.scale 由 attribute 系统驱动（registry key {@code minecraft:generic.scale}）。
 * 仅对该 viewer 发包（{@code sendServerPacket}），实现观察者侧缩放。</p>
 *
 * <p><b>风险</b>：ProtocolLib 对 1.21 attribute 包的 wrapper 命名在 PL 小版本间可能不同，
 * 因此构造时由 {@link #probe()} 探测能力，失败时 {@code ProtocolLibPresent} 退化为 {@code NoProtocolLib}。</p>
 */
final class ScalePackets {

    private static final UUID MODIFIER_UUID = UUID.fromString("a5503ec2-3bb8-4d55-b50e-4e1e4ff7a4e2");
    private static final String SCALE_KEY = "minecraft:generic.scale";

    private ScalePackets() {}

    /** capability 探测：试构属性与包结构，失败返回 false。 */
    static boolean probe() {
        try {
            buildAttribute(1.0, buildModifier(0.0));
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.UPDATE_ATTRIBUTES);
            packet.getAttributeCollectionModifier();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 给 viewer 发包：target 缩放为 scale（通过 ADD_NUMBER 修饰符 amount = scale - 1.0）。 */
    static void sendScale(Player viewer, Player target, float scale) {
        WrappedAttribute attribute = buildAttribute(1.0, buildModifier(scale - 1.0));
        PacketContainer packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.UPDATE_ATTRIBUTES);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getAttributeCollectionModifier().write(0, List.of(attribute));
        ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
    }

    /** 给 viewer 发包：还原 target 到默认 scale（base=1.0，无修饰符）。 */
    static void sendReset(Player viewer, Player target) {
        WrappedAttribute attribute = buildAttribute(1.0);
        PacketContainer packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.UPDATE_ATTRIBUTES);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getAttributeCollectionModifier().write(0, List.of(attribute));
        ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
    }

    private static WrappedAttributeModifier buildModifier(double amount) {
        return WrappedAttributeModifier.newBuilder()
                .uuid(MODIFIER_UUID)
                .name("parkour_scale")
                .amount(amount)
                .operation(WrappedAttributeModifier.Operation.ADD_NUMBER)
                .build();
    }

    private static WrappedAttribute buildAttribute(double base, WrappedAttributeModifier... modifiers) {
        WrappedAttribute.Builder builder = WrappedAttribute.newBuilder()
                .attributeKey(SCALE_KEY)
                .baseValue(base);
        if (modifiers.length > 0) {
            builder.modifiers(List.of(modifiers));
        }
        return builder.build();
    }
}
