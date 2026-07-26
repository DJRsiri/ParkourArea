package dev.aaf.parkourArea.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** 玩家中途存档点（mid=踩金块；last=最后记录点，用于「回到最后存档点」）。<b>同步阻塞，须在 async 线程调用。</b> */
public final class CheckpointDao {

    private final Database db;

    public CheckpointDao(Database db) {
        this.db = db;
    }

    public CheckpointData get(UUID uuid, int levelId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT mid_x, mid_y, mid_z, last_x, last_y, last_z, has_mid, has_last "
                             + "FROM player_checkpoint WHERE player_uuid=? AND level_zone_id=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return CheckpointData.empty();
                }
                return new CheckpointData(
                        rs.getInt(7) != 0, rs.getInt(1), rs.getInt(2), rs.getInt(3),
                        rs.getInt(8) != 0, rs.getInt(4), rs.getInt(5), rs.getInt(6)
                );
            }
        }
    }

    private void upsert(UUID uuid, int levelId, Updater updater) throws SQLException {
        try (Connection c = db.getConnection()) {
            // 先确保行存在
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT OR IGNORE INTO player_checkpoint(player_uuid, level_zone_id) VALUES(?,?)")) {
                ins.setBytes(1, Database.uuidToBytes(uuid));
                ins.setInt(2, levelId);
                ins.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE player_checkpoint SET mid_x=?, mid_y=?, mid_z=?, last_x=?, last_y=?, last_z=?, "
                            + "has_mid=?, has_last=? WHERE player_uuid=? AND level_zone_id=?")) {
                updater.apply(ps);
                ps.setBytes(9, Database.uuidToBytes(uuid));
                ps.setInt(10, levelId);
                ps.executeUpdate();
            }
        }
    }

    @FunctionalInterface
    private interface Updater {
        void apply(PreparedStatement ps) throws SQLException;
    }

    public void setMid(UUID uuid, int levelId, int x, int y, int z) throws SQLException {
        upsert(uuid, levelId, ps -> {
            ps.setInt(1, x); ps.setInt(2, y); ps.setInt(3, z);
            ps.setInt(4, x); ps.setInt(5, y); ps.setInt(6, z); // 同时作为 last
            ps.setInt(7, 1); ps.setInt(8, 1);
        });
    }

    /** 仅更新 last（用于回到存档点时的复位目标）。 */
    public void setLast(UUID uuid, int levelId, int x, int y, int z) throws SQLException {
        upsert(uuid, levelId, ps -> {
            ps.setInt(1, x); ps.setInt(2, y); ps.setInt(3, z); // mid 也更新为该点
            ps.setInt(4, x); ps.setInt(5, y); ps.setInt(6, z);
            ps.setInt(7, 1); ps.setInt(8, 1);
        });
    }

    public void clear(UUID uuid, int levelId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM player_checkpoint WHERE player_uuid=? AND level_zone_id=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            ps.executeUpdate();
        }
    }
}
