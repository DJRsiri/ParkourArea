package dev.aaf.parkourArea.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 内部事件总线。同步分发，线程安全（订阅列表用 COW）。
 *
 * <p>约定：{@link #publish(InternalEvent)} 由 global region 线程调用，
 * 订阅者也在该线程被回调，避免会话字段的并发问题。</p>
 */
public final class EventBus {

    private final Map<Class<? extends InternalEvent>, List<Consumer<? extends InternalEvent>>> subscribers =
            new ConcurrentHashMap<>();

    public <E extends InternalEvent> void subscribe(Class<E> type, Consumer<E> handler) {
        subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void publish(InternalEvent event) {
        List<Consumer<? extends InternalEvent>> list = subscribers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Consumer handler : list) {
            try {
                handler.accept(event);
            } catch (Throwable t) {
                // 单个订阅者异常不影响其他订阅者
                t.printStackTrace();
            }
        }
    }
}
