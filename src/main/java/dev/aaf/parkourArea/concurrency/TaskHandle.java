package dev.aaf.parkourArea.concurrency;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * 包装 {@link ScheduledTask}，提供平台无关的取消能力。
 * 若底层 task 为 null（例如一次性同步执行的任务），cancel 为空操作。
 */
public final class TaskHandle {

    private final ScheduledTask task;

    public TaskHandle(ScheduledTask task) {
        this.task = task;
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    public ScheduledTask raw() {
        return task;
    }
}
