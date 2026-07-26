package dev.aaf.parkourArea.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 玩家每关最佳（最短）用时。<b>同步阻塞，须在 async 线程调用。</b> */
public final class PlayerBestDao {

    private final Database db;

    public PlayerBestDao(Database db) {
        this.db = db;
    }

    public Long getBest(UUID uuid, int levelId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT best_millis FROM player_level_best WHERE player_uuid=? AND level_zone_id=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    /** 仅在新用时更短（或无记录）时更新最佳。返回是否更新。 */
    public boolean updateBestIfBetter(UUID uuid, int levelId, long durationMillis, long nowMillis) throws SQLException {
        Long existing = getBest(uuid, levelId);
        if (existing != null && durationMillis >= existing) {
            return false;
        }
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO player_level_best(player_uuid, level_zone_id, best_millis, updated_at) "
                             + "VALUES(?,?,?,?) ON CONFLICT(player_uuid, level_zone_id) DO UPDATE SET "
                             + "best_millis=excluded.best_millis, updated_at=excluded.updated_at")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            ps.setLong(3, durationMillis);
            ps.setLong(4, nowMillis);
            ps.executeUpdate();
        }
        return true;
    }

    public Map<Integer, Long> getAllBests(UUID uuid) throws SQLException {
        Map<Integer, Long> out = new HashMap<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT level_zone_id, best_millis FROM player_level_best WHERE player_uuid=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt(1), rs.getLong(2));
                }
            }
        }
        return out;
    }
}
