package dev.aaf.parkourArea.persistence;

import dev.aaf.parkourArea.player.VisibilityMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** 玩家偏好 DAO。<b>同步阻塞，须在 async 线程调用。</b> */
public final class PlayerPreferenceDao {

    private final Database db;

    public PlayerPreferenceDao(Database db) {
        this.db = db;
    }

    /** 读取玩家偏好；无记录返回 fallback。 */
    public Preference getOrDefault(UUID uuid, Preference fallback) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT sound_enabled, checkpoint_sound, block_sound, visibility_mode "
                             + "FROM player_preference WHERE player_uuid=?")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return fallback;
                }
                return new Preference(
                        rs.getInt(1) != 0,
                        rs.getInt(2) != 0,
                        rs.getInt(3) != 0,
                        VisibilityMode.parse(rs.getString(4), VisibilityMode.FULL)
                );
            }
        }
    }

    public void upsert(UUID uuid, Preference pref) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO player_preference(player_uuid, sound_enabled, checkpoint_sound, block_sound, visibility_mode) "
                             + "VALUES(?,?,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET "
                             + "sound_enabled=excluded.sound_enabled, checkpoint_sound=excluded.checkpoint_sound, "
                             + "block_sound=excluded.block_sound, visibility_mode=excluded.visibility_mode")) {
            ps.setBytes(1, Database.uuidToBytes(uuid));
            ps.setInt(2, pref.sound() ? 1 : 0);
            ps.setInt(3, pref.checkpoint() ? 1 : 0);
            ps.setInt(4, pref.block() ? 1 : 0);
            ps.setString(5, pref.mode().name());
            ps.executeUpdate();
        }
    }
}
