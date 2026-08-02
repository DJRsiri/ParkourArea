package dev.aaf.parkourArea.hooks.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 构造并给单个观察者发送 UPDATE_ATTRIBUTES 包，对目标实体注入 generic.scale 属性修饰符。
 *
 * <p>ProtocolLib 的 WrappedAttribute/WrappedRegistry 尚未适配 1.21.11：
 * NMS ResourceLocation 更名为 Identifier、独立 AttributeSnapshot 改为包内嵌 record，
 * 导致 WrappedRegistry 对任何属性 key 都返回 null（"Invalid attribute name"）。
 * 因此这里直接反射 NMS 构造原生包，ProtocolLib 仅作发包通道。</p>
 *
 * <p>反射目标（1.21.11 mojang mappings）：
 * {@code CraftAttribute.bukkitToMinecraftHolder} → Holder&lt;Attribute&gt;；
 * {@code Identifier.parse} + {@code AttributeModifier(Identifier, double, Operation)}；
 * {@code AttributeInstance(Holder, Consumer)} + setBaseValue/addTransientModifier；
 * {@code ClientboundUpdateAttributesPacket(int, Collection<AttributeInstance>)}。</p>
 */
final class ScalePackets {

    private static final String MODIFIER_ID = "parkourarea:scale";

    private static volatile Method bukkitToMinecraftHolder;
    private static volatile Method identifierParse;
    private static volatile Constructor<?> modifierCtor;
    private static volatile Constructor<?> instanceCtor;
    private static volatile Method setBaseValue;
    private static volatile Method addTransientModifier;
    private static volatile Constructor<?> packetCtor;
    private static volatile Object operationAddValue;

    private ScalePackets() {}

    /** capability 探测：初始化反射链路并试构完整原生包与 PacketContainer，失败返回 false（异常交由调用方记录）。 */
    static boolean probe(Logger logger) {
        try {
            init();
            PacketContainer.fromPacket(buildPacket(0, 1.0f));
            return true;
        } catch (Throwable t) {
            logger.warning("scale 包探测失败: " + t.getClass().getName() + ": " + t.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static synchronized void init() throws Exception {
        if (packetCtor != null) {
            return;
        }
        Class<?> craftAttribute = Class.forName("org.bukkit.craftbukkit.attribute.CraftAttribute");
        Class<?> identifier = Class.forName("net.minecraft.resources.Identifier");
        Class<?> holder = Class.forName("net.minecraft.core.Holder");
        Class<?> nmsModifier = Class.forName("net.minecraft.world.entity.ai.attributes.AttributeModifier");
        Class<?> nmsOperation = Class.forName("net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation");
        Class<?> nmsInstance = Class.forName("net.minecraft.world.entity.ai.attributes.AttributeInstance");
        Class<?> nmsPacket = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket");

        bukkitToMinecraftHolder = craftAttribute.getMethod("bukkitToMinecraftHolder", Attribute.class);
        identifierParse = identifier.getMethod("parse", String.class);
        modifierCtor = nmsModifier.getConstructor(identifier, double.class, nmsOperation);
        instanceCtor = nmsInstance.getConstructor(holder, Consumer.class);
        setBaseValue = nmsInstance.getMethod("setBaseValue", double.class);
        addTransientModifier = nmsInstance.getMethod("addTransientModifier", nmsModifier);
        packetCtor = nmsPacket.getConstructor(int.class, Collection.class);
        operationAddValue = Enum.valueOf(nmsOperation.asSubclass(Enum.class), "ADD_VALUE");
    }

    /** 构造 scale 包：base=1.0；scale != 1.0 时附带 ADD_VALUE 修饰符（amount = scale - 1.0）。 */
    private static Object buildPacket(int entityId, float scale) throws Exception {
        Object attributeHolder = bukkitToMinecraftHolder.invoke(null, Attribute.SCALE);
        Object instance = instanceCtor.newInstance(attributeHolder, (Consumer<Object>) dirty -> {});
        setBaseValue.invoke(instance, 1.0);
        if (scale != 1.0f) {
            Object id = identifierParse.invoke(null, MODIFIER_ID);
            Object modifier = modifierCtor.newInstance(id, (double) scale - 1.0, operationAddValue);
            addTransientModifier.invoke(instance, modifier);
        }
        return packetCtor.newInstance(entityId, List.of(instance));
    }

    /** 给 viewer 发包：target 缩放为 scale。 */
    static void sendScale(Player viewer, Player target, float scale) throws Exception {
        Object nmsPacket = buildPacket(target.getEntityId(), scale);
        ProtocolLibrary.getProtocolManager()
                .sendServerPacket(viewer, PacketContainer.fromPacket(nmsPacket));
    }

    /** 给 viewer 发包：还原 target 到默认 scale（base=1.0，无修饰符）。 */
    static void sendReset(Player viewer, Player target) throws Exception {
        Object nmsPacket = buildPacket(target.getEntityId(), 1.0f);
        ProtocolLibrary.getProtocolManager()
                .sendServerPacket(viewer, PacketContainer.fromPacket(nmsPacket));
    }
}
