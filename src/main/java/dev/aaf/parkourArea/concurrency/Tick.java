package dev.aaf.parkourArea.concurrency;

/** Tick 与时间单位的转换工具。 */
public final class Tick {

    /** 每秒 tick 数（Minecraft 标准）。 */
    public static final int TICKS_PER_SECOND = 20;

    private Tick() {}

    public static long secondsToTicks(double seconds) {
        return (long) Math.ceil(seconds * TICKS_PER_SECOND);
    }

    public static double ticksToSeconds(long ticks) {
        return ticks / (double) TICKS_PER_SECOND;
    }

    public static long millisToTicks(long millis) {
        return Math.max(1, millis * TICKS_PER_SECOND / 1000L);
    }

    public static long ticksToMillis(long ticks) {
        return ticks * 1000L / TICKS_PER_SECOND;
    }
}
