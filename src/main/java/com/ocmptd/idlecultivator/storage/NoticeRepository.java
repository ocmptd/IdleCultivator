package com.ocmptd.idlecultivator.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 待送达消息:主动推送不可用(QQ 被动消息 5 分钟时效)时暂存,
 * 玩家下次发消息时先带出再回复。
 */
public class NoticeRepository {
    private final Connection conn;

    public NoticeRepository(Database db) {
        this.conn = db.connection();
    }

    public void add(String userId, String message) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO pending_notices (user_id, message, created_at) VALUES (?,?,?)")) {
            ps.setString(1, userId);
            ps.setString(2, message);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("保存待送达消息失败", e);
        }
    }

    /** 取出并清空该玩家的所有待送达消息(按时间先后)。 */
    public List<String> drain(String userId) {
        List<String> messages = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT message FROM pending_notices WHERE user_id = ? ORDER BY notice_id")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) messages.add(rs.getString("message"));
            }
            if (!messages.isEmpty()) {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM pending_notices WHERE user_id = ?")) {
                    del.setString(1, userId);
                    del.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("读取待送达消息失败", e);
        }
        return messages;
    }
}
