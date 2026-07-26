package dev.aaf.parkourArea.hooks.papi;

import dev.aaf.parkourArea.ParkourArea;

/**
 * PAPI 注册入口（独立类，仅在安装 PlaceholderAPI 时由主类反射/直接调用，避免主类硬依赖 PlaceholderExpansion）。
 */
public final class PapiRegistration {

    private PapiRegistration() {}

    public static void register(ParkourArea plugin) {
        new PlaceholderHook(plugin).register();
    }
}
