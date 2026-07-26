package dev.aaf.parkourArea.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * SQLite 连接池（HikariCP）+ schema 迁移。
 *
 * <p><b>Folia 约定</b>：所有 DAO 方法都是同步阻塞的，<b>必须</b>在 async scheduler 线程上调用，
 * 绝不能在区域/实体线程上调用。连接池大小为 1（SQLite 写串行，避免 "database is locked"）。</p>
 */
public final class Database {

    private final HikariDataSource dataSource;

    public Database(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new RuntimeException("无法创建数据目录: " + dataFolder);
        }
        File dbFile = new File(dataFolder, "parkour.db");
        // 用正斜杠避免 JDBC URL 反斜杠转义问题
        String path = dbFile.getAbsolutePath().replace('\\', '/');

        HikariConfig config = new HikariConfig();
        config.setPoolName("ParkourArea-Hikari");
        config.setJdbcUrl("jdbc:sqlite:" + path);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("foreign_keys", "true");
        config.setMaxLifetime(0); // SQLite 连接长期持有

        this.dataSource = new HikariDataSource(config);
        migrate(plugin);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void migrate(JavaPlugin plugin) {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            int version = 0;
            try (ResultSet rs = s.executeQuery("PRAGMA user_version")) {
                if (rs.next()) {
                    version = rs.getInt(1);
                }
            }
            if (version < 1) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS player_level_time ("
                        + "player_uuid BLOB NOT NULL,"
                        + "level_zone_id INTEGER NOT NULL,"
                        + "attempt_idx INTEGER NOT NULL,"
                        + "duration_millis BIGINT NOT NULL,"
                        + "finished_at BIGINT NOT NULL,"
                        + "PRIMARY KEY (player_uuid, level_zone_id, attempt_idx))");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS player_level_best ("
                        + "player_uuid BLOB NOT NULL,"
                        + "level_zone_id INTEGER NOT NULL,"
                        + "best_millis BIGINT NOT NULL,"
                        + "updated_at BIGINT NOT NULL,"
                        + "PRIMARY KEY (player_uuid, level_zone_id))");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS player_level_progress ("
                        + "player_uuid BLOB NOT NULL,"
                        + "level_zone_id INTEGER NOT NULL,"
                        + "status TEXT NOT NULL DEFAULT 'NONE',"
                        + "last_visited_start_at BIGINT,"
                        + "PRIMARY KEY (player_uuid, level_zone_id))");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS player_checkpoint ("
                        + "player_uuid BLOB NOT NULL,"
                        + "level_zone_id INTEGER NOT NULL,"
                        + "mid_x INTEGER, mid_y INTEGER, mid_z INTEGER,"
                        + "last_x INTEGER, last_y INTEGER, last_z INTEGER,"
                        + "has_mid INTEGER NOT NULL DEFAULT 0,"
                        + "has_last INTEGER NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (player_uuid, level_zone_id))");
                s.executeUpdate("PRAGMA user_version = 1");
                plugin.getLogger().info("数据库已初始化至 schema v1");
            }
            if (version < 2) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS player_preference ("
                        + "player_uuid BLOB PRIMARY KEY,"
                        + "sound_enabled INTEGER NOT NULL DEFAULT 1,"
                        + "checkpoint_sound INTEGER NOT NULL DEFAULT 1,"
                        + "block_sound INTEGER NOT NULL DEFAULT 1,"
                        + "visibility_mode TEXT NOT NULL DEFAULT 'FULL')");
                s.executeUpdate("PRAGMA user_version = 2");
                plugin.getLogger().info("数据库已迁移至 schema v2 (player_preference)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库迁移失败", e);
        }
    }

    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ---- UUID <-> BLOB 转换 ----

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID bytesToUuid(byte[] b) {
        if (b == null || b.length != 16) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(b);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
