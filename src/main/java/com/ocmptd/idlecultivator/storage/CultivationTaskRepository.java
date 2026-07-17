package com.ocmptd.idlecultivator.storage;

import com.ocmptd.idlecultivator.game.cultivation.CultivationTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CultivationTaskRepository {
    private final Connection conn;

    public CultivationTaskRepository(Database db) {
        this.conn = db.connection();
    }

    public long insert(String userId, String groupId, String method, long startTime, int durationMinutes, long expectedReward) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO cultivation_tasks (user_id, group_id, method, start_time, duration_minutes, status, expected_reward, overflow_penalty)
                VALUES (?,?,?,?,?,0,?,0)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, userId);
            ps.setString(2, groupId);
            ps.setString(3, method);
            ps.setLong(4, startTime);
            ps.setInt(5, durationMinutes);
            ps.setLong(6, expectedReward);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("创建修炼任务失败", e);
        }
    }

    /** 查找进行中(0)或待收获(3)的任务 */
    public Optional<CultivationTask> findActiveByUser(String userId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM cultivation_tasks WHERE user_id = ? AND status IN (0, 3) LIMIT 1")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询修炼任务失败", e);
        }
    }

    public List<CultivationTask> findFinishedRunning(long now) {
        List<CultivationTask> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM cultivation_tasks WHERE status = 0 AND (start_time + duration_minutes * 60000) <= ?")) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询到期修炼任务失败", e);
        }
        return list;
    }

    public void updateStatus(long taskId, int status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE cultivation_tasks SET status = ? WHERE task_id = ?")) {
            ps.setInt(1, status);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新修炼任务失败", e);
        }
    }

    private CultivationTask map(ResultSet rs) throws SQLException {
        return new CultivationTask(
                rs.getLong("task_id"),
                rs.getString("user_id"),
                rs.getString("group_id"),
                rs.getString("method"),
                rs.getLong("start_time"),
                rs.getInt("duration_minutes"),
                rs.getInt("status"),
                rs.getLong("expected_reward"),
                rs.getDouble("overflow_penalty"));
    }
}
