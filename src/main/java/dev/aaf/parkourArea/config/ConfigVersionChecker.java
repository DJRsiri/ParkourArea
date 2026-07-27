package dev.aaf.parkourArea.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置文件版本检测与备份/合并工具。
 *
 * <p>四个 jar 模板（config/messages/blocks/ratings.yml）顶层各带版本键；
 * 数据目录文件无版本键视为 1。zones.yml 是运行时数据，不参与版本管理。</p>
 */
public final class ConfigVersionChecker {

    /** 文件名 → 版本键（LinkedHashMap 保序，输出稳定）。 */
    public static final Map<String, String> VERSION_KEYS;
    /** 文件名 → jar 内置期望版本。 */
    public static final Map<String, Integer> EXPECTED_VERSIONS;

    static {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("config.yml", "config-version");
        keys.put("messages.yml", "messages-version");
        keys.put("blocks.yml", "blocks-version");
        keys.put("ratings.yml", "ratings-version");
        VERSION_KEYS = Collections.unmodifiableMap(keys);
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("config.yml", 2);
        expected.put("messages.yml", 2);
        expected.put("blocks.yml", 2);
        expected.put("ratings.yml", 2);
        EXPECTED_VERSIONS = Collections.unmodifiableMap(expected);
    }

    /** 一个过旧文件：文件名 / 当前版本 / 期望版本。 */
    public record Outdated(String file, int current, int expected) {}

    private final JavaPlugin plugin;
    private volatile List<Outdated> outdated = List.of();

    public ConfigVersionChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 重新检测所有模板文件版本并刷新内存清单；返回本次检测结果。 */
    public List<Outdated> check() {
        Map<String, Integer> current = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : VERSION_KEYS.entrySet()) {
            current.put(e.getKey(),
                    readVersion(new File(plugin.getDataFolder(), e.getKey()), e.getValue()));
        }
        this.outdated = List.copyOf(diffVersions(current));
        return this.outdated;
    }

    /** 上次检测的过旧清单（join 提示用）。 */
    public List<Outdated> outdated() {
        return outdated;
    }

    /** 对比当前版本与期望版本，返回过旧清单（纯函数）。 */
    public static List<Outdated> diffVersions(Map<String, Integer> currentVersions) {
        List<Outdated> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : EXPECTED_VERSIONS.entrySet()) {
            int current = currentVersions.getOrDefault(e.getKey(), 1);
            if (current < e.getValue()) {
                result.add(new Outdated(e.getKey(), current, e.getValue()));
            }
        }
        return result;
    }

    /** 读文件版本：文件不存在或无版本键（含损坏）视为 1。 */
    public static int readVersion(File file, String versionKey) {
        if (!file.exists()) {
            return 1;
        }
        try {
            return loadStrict(file).getInt(versionKey, 1);
        } catch (IOException | InvalidConfigurationException e) {
            return 1;
        }
    }

    /**
     * 显式 load（吞异常版本无法区分损坏与空文件）。
     * Bukkit {@code YamlConfiguration.loadConfiguration} 会吞掉解析异常，这里用
     * {@code cfg.load(file)} 让 {@link InvalidConfigurationException} 抛给调用方。
     */
    public static YamlConfiguration loadStrict(File file)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.load(file);
        return cfg;
    }

    /** 计算下一个备份文件：config.yml → config.yml.bak；已存在则 .bak1、.bak2… */
    public static File nextBackupFile(File source) {
        // 调用方均为数据目录内文件，必有父目录；getParentFile() 为 null 时退化为相对 CWD
        File bak = new File(source.getParentFile(), source.getName() + ".bak");
        if (!bak.exists()) {
            return bak;
        }
        for (int i = 1; ; i++) {
            File f = new File(source.getParentFile(), source.getName() + ".bak" + i);
            if (!f.exists()) {
                return f;
            }
        }
    }

    /**
     * 标准 defaults 合并：以 jar 内资源为 defaults，旧文件用户值优先，新版新增键补入。
     *
     * @return true 合并成功；false 旧文件损坏无法解析（调用方应提示 recreateconf）
     */
    public boolean mergeWithDefaults(String resourceName) throws IOException {
        File target = new File(plugin.getDataFolder(), resourceName);
        YamlConfiguration old;
        try {
            old = loadStrict(target);
        } catch (InvalidConfigurationException e) {
            return false;
        }
        YamlConfiguration defaults = new YamlConfiguration();
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) {
                throw new IOException("jar 内缺少资源: " + resourceName);
            }
            defaults.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException e) {
            throw new IOException("jar 内资源损坏: " + resourceName, e);
        }
        return mergeWithDefaults(target, old, defaults);
    }

    /** 合并并保存（测试友好重载：直接传 defaults）。 */
    static boolean mergeWithDefaults(File target, YamlConfiguration defaults) throws IOException {
        YamlConfiguration old;
        try {
            old = loadStrict(target);
        } catch (InvalidConfigurationException e) {
            return false;
        }
        return mergeWithDefaults(target, old, defaults);
    }

    private static boolean mergeWithDefaults(File target, YamlConfiguration old,
                                             YamlConfiguration defaults) throws IOException {
        old.setDefaults(defaults);
        old.options().copyDefaults(true);
        // 版本键强制刷新为 defaults 值：用户值优先的规则对版本键不适用，
        // 合并完成即代表文件已升级到新版本
        for (String versionKey : VERSION_KEYS.values()) {
            if (defaults.contains(versionKey)) {
                old.set(versionKey, defaults.get(versionKey));
            }
        }
        old.save(target);
        return true;
    }

    /** 供命令使用：数据目录内某模板文件。 */
    public File fileOf(String resourceName) {
        return new File(plugin.getDataFolder(), resourceName);
    }

    /** 供命令使用：触发插件 saveResource 重建。 */
    public void saveResource(String resourceName) {
        plugin.saveResource(resourceName, false);
    }
}
