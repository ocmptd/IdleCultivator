package com.ocmptd.idlecultivator.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 社交仓储:管理互助记录(mutual_helps)与道侣关系(dao_companions)。
 */
public class SocialRepository {
    private final Connection conn;

    public SocialRepository(Database db) {
        this.conn = db.connection();
    }

    // ==================== 互助记录 ====================

    /** 添加一条互助记录,返回当前玩家有效的互助次数(含本次)。 */
    public void addMutualHelp(String userId, String targetId, String groupId, long expireTime) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO mutual_helps (user_id, target_id, group_id, expire_time) VALUES (?,?,?,?)")) {
            ps.setString(1, userId);
            ps.setString(2, targetId);
            ps.setString(3, groupId);
            ps.setLong(4, expireTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("添加互助记录失败", e);
        }
    }

    /** 统计玩家当前有效的互助加成次数(作为发起者或目标)。 */
    public int countActiveHelps(String userId, long now) {
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM mutual_helps WHERE (user_id = ? OR target_id = ?) AND expire_time > ?")) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setLong(3, now);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) count = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询互助次数失败", e);
        }
        return Math.min(count, 3);
    }

    /** 检查是否已有互助记录(避免短时间重复互助同一人)。 */
    public boolean hasActiveHelp(String userId, String targetId, long now) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM mutual_helps WHERE user_id = ? AND target_id = ? AND expire_time > ? LIMIT 1")) {
            ps.setString(1, userId);
            ps.setString(2, targetId);
            ps.setLong(3, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询互助记录失败", e);
        }
    }

    /** 清理过期互助记录。 */
    public void cleanExpiredHelps(long now) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM mutual_helps WHERE expire_time <= ?")) {
            ps.setLong(1, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("清理过期互助记录失败", e);
        }
    }

    /** 获取玩家当前有效的互助详情列表。 */
    public List<String> activeHelpDetails(String userId, long now) {
        List<String> details = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT user_id, target_id, expire_time FROM mutual_helps WHERE (user_id = ? OR target_id = ?) AND expire_time > ? ORDER BY expire_time")) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setLong(3, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long remain = (rs.getLong("expire_time") - now) / 60_000;
                    details.add(rs.getString("user_id") + "→" + rs.getString("target_id") + " 剩余" + remain + "分钟");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询互助详情失败", e);
        }
        return details;
    }

    // ==================== 道侣关系 ====================

    public Optional<String> findPartnerId(String userId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT partner_id FROM dao_companions WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("partner_id")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询道侣失败", e);
        }
    }

    public void addDaoCompanion(String userId, String partnerId, String groupId, long establishedAt) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dao_companions (user_id, partner_id, group_id, established_at) VALUES (?,?,?,?) " +
                        "ON CONFLICT(user_id) DO UPDATE SET partner_id=excluded.partner_id, group_id=excluded.group_id, established_at=excluded.established_at")) {
            ps.setString(1, userId);
            ps.setString(2, partnerId);
            ps.setString(3, groupId);
            ps.setLong(4, establishedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("添加道侣关系失败", e);
        }
    }

    public void removeDaoCompanion(String userId) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dao_companions WHERE user_id = ?")) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("删除道侣关系失败", e);
        }
    }
}
