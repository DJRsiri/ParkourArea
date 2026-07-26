package dev.aaf.parkourArea.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 玩家每关前 N 次通关用时记录（循环覆盖最旧，保留最新 N 次）。
 * <b>同步阻塞，须在 async 线程调用。</b>
 */
public final class PlayerTimeDao {

    private final Database db;
    private final int maxRecords;

    public PlayerTimeDao(Database db, int maxRecords) {
        this.db = db;
        this.maxRecords = Math.max(1, maxRecords);
    }

    /** 记录一次通关用时，循环覆盖最旧记录以保留最新 N 次。 */
    public void addTime(UUID uuid, int levelId, long durationMillis, long finishedAtMillis) throws SQLException {
        byte[] key = Database.uuidToBytes(uuid);
        try (Connection c = db.getConnection()) {
            int count = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM player_level_time WHERE player_uuid=? AND level_zone_id=?")) {
                ps.setBytes(1, key);
                ps.setInt(2, levelId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
            int targetIdx;
            if (count < maxRecords) {
                targetIdx = count; // 新槽位
            } else {
                // 替换最旧记录的 attempt_idx
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT attempt_idx FROM player_level_time WHERE player_uuid=? AND level_zone_id=? "
                                + "ORDER BY finished_at ASC LIMIT 1")) {
                    ps.setBytes(1, key);
                    ps.setInt(2, levelId);
                    try (ResultSet rs = ps.executeQuery()) {
                        targetIdx = rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO player_level_time(player_uuid, level_zone_id, attempt_idx, duration_millis, finished_at) "
                            + "VALUES(?,?,?,?,?) ON CONFLICT(player_uuid, level_zone_id, attempt_idx) DO UPDATE SET "
                            + "duration_millis=excluded.duration_millis, finished_at=excluded.finished_at")) {
                ps.setBytes(1, key);
                ps.setInt(2, levelId);
                ps.setInt(3, targetIdx);
                ps.setLong(4, durationMillis);
                ps.setLong(5, finishedAtMillis);
                ps.executeUpdate();
            }
        }
    }

    /** 取该玩家该关的前 N 次用时（按完成时间升序）。 */
    public List<Long> getTimes(UUID uuid, int levelId) throws SQLException {
        List<Long> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT duration_millis FROM player_level_time WHERE player_uuid=? AND level_zone_id=? "
                             + "ORDER BY finished_at ASC")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, levelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getLong(1));
                }
            }
        }
        return out;
    }
}
