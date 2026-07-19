package com.ocmptd.idlecultivator.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 宗门战仓储:管理 sect_war_weekly 表,按周统计各群修炼总修为。
 */
public class SectWarRepository {
    private final Connection conn;

    public SectWarRepository(Database db) {
        this.conn = db.connection();
    }

    /** 累加某群本周修为。 */
    public void addExp(String weekKey, String groupId, long exp) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO sect_war_weekly (week_key, group_id, total_exp)
                VALUES (?,?,?)
                ON CONFLICT(week_key, group_id) DO UPDATE SET
                    total_exp=sect_war_weekly.total_exp + excluded.total_exp
                """)) {
            ps.setString(1, weekKey);
            ps.setString(2, groupId);
            ps.setLong(3, exp);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新宗门战修为失败", e);
        }
    }

    /** 获取本周排行(按修为降序,最多前10名)。 */
    public List<Entry> ranking(String weekKey, int limit) {
        List<Entry> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT group_id, total_exp FROM sect_war_weekly WHERE week_key = ? ORDER BY total_exp DESC LIMIT ?")) {
            ps.setString(1, weekKey);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Entry(rs.getString("group_id"), rs.getLong("total_exp")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询宗门战排行失败", e);
        }
        return list;
    }

    /** 查询某群本周修为排名。 */
    public int rankOf(String weekKey, String groupId) {
        List<Entry> ranking = ranking(weekKey, 100);
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).groupId().equals(groupId)) return i + 1;
        }
        return 0;
    }

    /** 获取上周排行(用于结算公告)。 */
    public List<Entry> lastWeekRanking(String lastWeekKey) {
        return ranking(lastWeekKey, 10);
    }

    public record Entry(String groupId, long totalExp) {}
}
