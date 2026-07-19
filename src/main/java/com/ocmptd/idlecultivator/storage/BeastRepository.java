package com.ocmptd.idlecultivator.storage;

import com.ocmptd.idlecultivator.game.social.Beast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * 异兽仓储:管理 group_beasts 表。
 */
public class BeastRepository {
    private final Connection conn;

    public BeastRepository(Database db) {
        this.conn = db.connection();
    }

    public Optional<Beast> findByGroupId(String groupId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM group_beasts WHERE group_id = ?")) {
            ps.setString(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询异兽失败", e);
        }
    }

    public void save(Beast beast) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO group_beasts (group_id, beast_name, hp, max_hp, exp_reward, stone_reward, spawn_time)
                VALUES (?,?,?,?,?,?,?)
                ON CONFLICT(group_id) DO UPDATE SET
                    beast_name=excluded.beast_name,
                    hp=excluded.hp,
                    max_hp=excluded.max_hp,
                    exp_reward=excluded.exp_reward,
                    stone_reward=excluded.stone_reward,
                    spawn_time=excluded.spawn_time
                """)) {
            ps.setString(1, beast.groupId());
            ps.setString(2, beast.name());
            ps.setInt(3, beast.hp());
            ps.setInt(4, beast.maxHp());
            ps.setLong(5, beast.expReward());
            ps.setLong(6, beast.stoneReward());
            ps.setLong(7, beast.spawnTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("保存异兽失败", e);
        }
    }

    public void delete(String groupId) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM group_beasts WHERE group_id = ?")) {
            ps.setString(1, groupId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("删除异兽失败", e);
        }
    }

    private Beast map(ResultSet rs) throws SQLException {
        return new Beast(
                rs.getString("group_id"),
                rs.getString("beast_name"),
                rs.getInt("max_hp"),
                rs.getInt("hp"),
                rs.getLong("exp_reward"),
                rs.getLong("stone_reward"),
                rs.getLong("spawn_time"));
    }
}
