package dev.aaf.parkourArea.zone;

/** 区域数字 ID 分配器（自增；支持手动指定；重载后可重排顺序）。 */
public final class ZoneIdAllocator {

    private int next;

    public ZoneIdAllocator(int start) {
        this.next = start;
    }

    public int next() {
        return next++;
    }

    public int peek() {
        return next;
    }

    /** 占用指定 ID（手动指定时），并保证 next 跳过它。 */
    public void reserve(int id) {
        if (id >= next) {
            next = id + 1;
        }
    }

    public void setNext(int value) {
        this.next = value;
    }
}
