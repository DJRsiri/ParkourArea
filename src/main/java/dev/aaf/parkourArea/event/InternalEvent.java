package dev.aaf.parkourArea.event;

import java.util.UUID;

/**
 * 插件内部事件基类。
 *
 * <p>由 {@link EventBus} 同步分发；所有订阅者假定在 global region 线程上被调用
 * （发布方需保证从该线程 publish）。</p>
 */
public abstract class InternalEvent {

    /** 相关玩家（可为 null，若事件不绑定特定玩家）。 */
    public UUID playerId() {
        return null;
    }
}
