package dev.aaf.parkourArea.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 二次确认会话管理。
 *
 * <p>用于 {@code /parkour delete} 等危险操作：首次执行 request 记录意图，玩家再次执行同命令时
 * 调用 confirm 完成确认。带 TTL，过期自动取消。</p>
 */
public final class ConfirmFlow {

    public static final long DEFAULT_TTL_MILLIS = 30_000L;

    private final Map<UUID, Pending> pendings = new HashMap<>();
    private final long ttlMillis;

    public ConfirmFlow() {
        this(DEFAULT_TTL_MILLIS);
    }

    public ConfirmFlow(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    private static final class Pending {
        final Consumer<Boolean> action; // true=确认执行；false=超时/取消
        final long expireAt;

        Pending(Consumer<Boolean> action, long expireAt) {
            this.action = action;
            this.expireAt = expireAt;
        }
    }

    public synchronized void request(UUID playerId, Consumer<Boolean> action) {
        pendings.put(playerId, new Pending(action, System.currentTimeMillis() + ttlMillis));
    }

    /** @return true 表示存在有效待确认项并已触发；false 表示无待确认或已过期。 */
    public synchronized boolean confirm(UUID playerId) {
        Pending p = pendings.remove(playerId);
        if (p == null) {
            return false;
        }
        if (System.currentTimeMillis() > p.expireAt) {
            p.action.accept(false);
            return false;
        }
        p.action.accept(true);
        return true;
    }

    public synchronized void cancel(UUID playerId) {
        Pending p = pendings.remove(playerId);
        if (p != null) {
            p.action.accept(false);
        }
    }

    public synchronized boolean hasPending(UUID playerId) {
        Pending p = pendings.get(playerId);
        return p != null && System.currentTimeMillis() <= p.expireAt;
    }

    /** 清理已过期的待确认项（可周期性调用）。 */
    public synchronized void purgeExpired() {
        long now = System.currentTimeMillis();
        pendings.entrySet().removeIf(e -> {
            if (now > e.getValue().expireAt) {
                e.getValue().action.accept(false);
                return true;
            }
            return false;
        });
    }
}
