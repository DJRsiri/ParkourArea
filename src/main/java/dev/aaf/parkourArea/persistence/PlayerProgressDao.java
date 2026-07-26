package dev.aaf.parkourArea.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 玩家通关进度（NONE/VISITED/COMPLETED），用于选关灰黄绿与跳关判定。<b>同步阻塞，须在 async 线程调用。</b> */
public final class PlayerProgressDao {

    private final Database db;

    public PlayerProgressDao(Database db) {
        this.db = db;
    }

    public ProgressStatus getStatus(UUID uuid, int levelId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT status FROM player_level_progress WHERE player_uuid=? AND level_zone_id=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ProgressStatus.parse(rs.getString(1)) : ProgressStatus.NONE;
            }
        }
    }

    public void setStatus(UUID uuid, int levelId, ProgressStatus status, Long visitedAtMillis) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO player_level_progress(player_uuid, level_zone_id, status, last_visited_start_at) "
                             + "VALUES(?,?,?,?) ON CONFLICT(player_uuid, level_zone_id) DO UPDATE SET "
                             + "status=excluded.status, last_visited_start_at=excluded.last_visited_start_at")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            ps.setString(3, status.name());
            if (visitedAtMillis != null) {
                ps.setLong(4, visitedAtMillis);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.executeUpdate();
        }
    }

    public Map<Integer, ProgressStatus> getAllStatuses(UUID uuid) throws SQLException {
        Map<Integer, ProgressStatus> out = new HashMap<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT level_zone_id, status FROM player_level_progress WHERE player_uuid=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt(1), ProgressStatus.parse(rs.getString(2)));
                }
            }
        }
        return out;
    }

    /** 删档：清除玩家在某关的全部进度（status 回到 NONE）。 */
    public void clearProgress(UUID uuid, int levelId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM player_level_progress WHERE player_uuid=? AND level_zone_id=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            ps.executeUpdate();
        }
    }

    /** 删档时一并清除该关的计时/最佳记录。 */
    public void clearStats(UUID uuid, int levelId) throws SQLException {
        try (Connection c = db.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM player_level_time WHERE player_uuid=? AND level_zone_id=?")) {
                ps.setBytes(1, Database.uuidToBytes(uuid));
                ps.setInt(2, levelId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM player_level_best WHERE player_uuid=? AND level_zone_id=?")) {
                ps.setBytes(1, Database.uuidToBytes(uuid));
                ps.setInt(2, levelId);
                ps.executeUpdate();
            }
        }
    }
}
