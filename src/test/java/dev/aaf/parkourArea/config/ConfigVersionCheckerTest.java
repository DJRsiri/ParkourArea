package dev.aaf.parkourArea.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigVersionCheckerTest {

    @TempDir
    Path tempDir;

    @Test
    void nextBackupFileIncrements() throws IOException {
        File source = tempDir.resolve("config.yml").toFile();
        source.createNewFile();
        File bak0 = ConfigVersionChecker.nextBackupFile(source);
        assertThat(bak0.getName()).isEqualTo("config.yml.bak");
        bak0.createNewFile();
        File bak1 = ConfigVersionChecker.nextBackupFile(source);
        assertThat(bak1.getName()).isEqualTo("config.yml.bak1");
        bak1.createNewFile();
        assertThat(ConfigVersionChecker.nextBackupFile(source).getName()).isEqualTo("config.yml.bak2");
    }

    @Test
    void readVersionMissingFileIsOne() {
        assertThat(ConfigVersionChecker.readVersion(
                tempDir.resolve("nope.yml").toFile(), "config-version")).isEqualTo(1);
    }

    @Test
    void readVersionMissingKeyIsOne() throws IOException {
        File f = tempDir.resolve("config.yml").toFile();
        Files.writeString(f.toPath(), "settings:\n  a: 1\n");
        assertThat(ConfigVersionChecker.readVersion(f, "config-version")).isEqualTo(1);
    }

    @Test
    void readVersionReadsKey() throws IOException {
        File f = tempDir.resolve("config.yml").toFile();
        Files.writeString(f.toPath(), "config-version: 2\nsettings: {}\n");
        assertThat(ConfigVersionChecker.readVersion(f, "config-version")).isEqualTo(2);
    }

    @Test
    void loadStrictThrowsOnCorrupt() throws IOException {
        File f = tempDir.resolve("bad.yml").toFile();
        Files.writeString(f.toPath(), "key: [unclosed\n");
        assertThatThrownBy(() -> ConfigVersionChecker.loadStrict(f))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void diffVersionsMarksOutdated() {
        List<ConfigVersionChecker.Outdated> none = ConfigVersionChecker.diffVersions(Map.of(
                "config.yml", 2, "messages.yml", 3, "blocks.yml", 2, "ratings.yml", 2));
        assertThat(none).isEmpty();

        List<ConfigVersionChecker.Outdated> some = ConfigVersionChecker.diffVersions(Map.of(
                "config.yml", 1, "messages.yml", 3, "blocks.yml", 2, "ratings.yml", 2));
        assertThat(some).hasSize(1);
        assertThat(some.get(0).file()).isEqualTo("config.yml");
        assertThat(some.get(0).current()).isEqualTo(1);
        assertThat(some.get(0).expected()).isEqualTo(2);
    }

    @Test
    void mergeKeepsUserValuesAndAddsNewKeys() throws Exception {
        File target = tempDir.resolve("config.yml").toFile();
        Files.writeString(target.toPath(), "config-version: 1\nsettings:\n  debug: true\n");
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 2);
        defaults.set("settings.debug", false);
        defaults.set("settings.new-key", "hello");

        boolean ok = ConfigVersionChecker.mergeWithDefaults(target, defaults);
        assertThat(ok).isTrue();
        YamlConfiguration merged = ConfigVersionChecker.loadStrict(target);
        assertThat(merged.getBoolean("settings.debug")).isTrue();        // 用户值保留
        assertThat(merged.getString("settings.new-key")).isEqualTo("hello"); // 新键补入
        assertThat(merged.getInt("config-version")).isEqualTo(2);       // 版本键刷新
    }

    @Test
    void readVersionCorruptFileIsOne() throws IOException {
        File f = tempDir.resolve("bad.yml").toFile();
        Files.writeString(f.toPath(), "key: [unclosed\n");
        assertThat(ConfigVersionChecker.readVersion(f, "config-version")).isEqualTo(1);
    }

    @Test
    void mergeReturnsFalseOnCorruptOldFile() throws Exception {
        File target = tempDir.resolve("config.yml").toFile();
        String corrupt = "key: [unclosed\n";
        Files.writeString(target.toPath(), corrupt);
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 2);

        assertThat(ConfigVersionChecker.mergeWithDefaults(target, defaults)).isFalse();
        // 合并失败不得改写原文件（留给 recreateconf 流程处理）
        assertThat(Files.readString(target.toPath())).isEqualTo(corrupt);
    }
}
