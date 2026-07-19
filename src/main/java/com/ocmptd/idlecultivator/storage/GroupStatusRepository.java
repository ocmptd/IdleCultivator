package com.ocmptd.idlecultivator.storage;

import com.ocmptd.idlecultivator.game.social.GroupStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 群状态仓储:管理 group_status 表的 CRUD。
 */
public class GroupStatusRepository {
    private final Connection conn;

    public GroupStatusRepository(Database db) {
        this.conn = db.connection();
    }

    public Optional<GroupStatus> findById(String groupId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM group_status WHERE group_id = ?")) {
            ps.setString(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询群状态失败", e);
        }
    }

    /** 获取或创建群状态记录。 */
    public GroupStatus getOrCreate(String groupId) {
        return findById(groupId).orElseGet(() -> {
            GroupStatus gs = new GroupStatus(groupId);
            gs.setLastActiveTime(System.currentTimeMillis());
            save(gs);
            return gs;
        });
    }

    public void save(GroupStatus gs) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO group_status (group_id, last_active_time, message_count, cultivation_bonus,
                                           next_bonus_time, activity_level, hourly_msg_count, tide_until, last_tide_date)
                VALUES (?,?,?,1.0,NULL,?,?,?,?)
                ON CONFLICT(group_id) DO UPDATE SET
                    last_active_time=excluded.last_active_time,
                    message_count=excluded.message_count,
                    activity_level=excluded.activity_level,
                    hourly_msg_count=excluded.hourly_msg_count,
                    tide_until=excluded.tide_until,
                    last_tide_date=excluded.last_tide_date
                """)) {
            ps.setString(1, gs.groupId());
            ps.setLong(2, gs.lastActiveTime());
            ps.setInt(3, gs.messageCount());
            ps.setInt(4, gs.activityLevel());
            ps.setInt(5, gs.hourlyMsgCount());
            ps.setLong(6, gs.tideUntil());
            ps.setString(7, gs.lastTideDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("保存群状态失败", e);
        }
    }

    /** 查询所有群状态。 */
    public List<GroupStatus> findAll() {
        List<GroupStatus> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM group_status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("查询所有群状态失败", e);
        }
        return list;
    }

    /** 增加消息计数(总消息数+1,小时消息数+1),并更新最后活跃时间。 */
    public void incrementMessageCount(String groupId) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO group_status (group_id, last_active_time, message_count, hourly_msg_count, activity_level)
                VALUES (?, ?, 1, 1, 1)
                ON CONFLICT(group_id) DO UPDATE SET
                    last_active_time=excluded.last_active_time,
                    message_count=group_status.message_count + 1,
                    hourly_msg_count=group_status.hourly_msg_count + 1
                """)) {
            ps.setString(1, groupId);
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新群消息计数失败", e);
        }
    }

    private GroupStatus map(ResultSet rs) throws SQLException {
        GroupStatus gs = new GroupStatus(rs.getString("group_id"));
        gs.setLastActiveTime(rs.getLong("last_active_time"));
        gs.setMessageCount(rs.getInt("message_count"));
        gs.setActivityLevel(rs.getInt("activity_level"));
        gs.setHourlyMsgCount(rs.getInt("hourly_msg_count"));
        gs.setTideUntil(rs.getLong("tide_until"));
        gs.setLastTideDate(rs.getString("last_tide_date"));
        return gs;
    }
}
