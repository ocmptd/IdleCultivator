package com.ocmptd.idlecultivator.storage;

import com.ocmptd.idlecultivator.game.player.CultivationDirection;
import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.Realm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PlayerRepository {
    private final Connection conn;

    public PlayerRepository(Database db) {
        this.conn = db.connection();
    }

    public Optional<Player> findById(String userId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Player p = new Player(userId);
                p.setGroupId(rs.getString("group_id"));
                p.setName(rs.getString("name"));
                String gender = rs.getString("gender");
                p.setGender(gender == null ? null : Gender.fromLabel(gender));
                p.setRealm(Realm.fromLabel(rs.getString("level")));
                p.setExp(rs.getLong("current_exp"));
                p.setSpiritStones(rs.getLong("spirit_stones"));
                p.setEquipment(rs.getString("equipment"));
                p.setInventory(rs.getString("inventory"));
                String dir = rs.getString("cultivation_direction");
                p.setDirection(dir == null ? null : CultivationDirection.fromLabel(dir));
                p.setActiveStreak(rs.getInt("active_streak"));
                p.setNameChangedAt(rs.getLong("name_changed_at"));
                p.setLevel(rs.getInt("char_level"));
                p.setSeason(rs.getInt("season"));
                p.setCreatedAt(rs.getLong("created_at"));
                try { p.setLastActiveTime(rs.getLong("last_active_time")); } catch (SQLException ignored) {}
                try { p.setHairstyle(rs.getString("hairstyle")); } catch (SQLException ignored) {}
                try { p.setOutfit(rs.getString("outfit")); } catch (SQLException ignored) {}
                try { p.setAccessory(rs.getString("accessory")); } catch (SQLException ignored) {}
                try { p.setOverflowProtected(rs.getInt("overflow_protected") != 0); } catch (SQLException ignored) {}
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询玩家失败", e);
        }
    }

    public void save(Player p) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (user_id, group_id, name, gender, level, current_exp, spirit_stones,
                                   equipment, inventory, cultivation_direction, active_streak, name_changed_at,
                                   char_level, season, created_at, last_active_time,
                                   hairstyle, outfit, accessory, overflow_protected)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(user_id) DO UPDATE SET
                    group_id=excluded.group_id, name=excluded.name, gender=excluded.gender,
                    level=excluded.level, current_exp=excluded.current_exp, spirit_stones=excluded.spirit_stones,
                    equipment=excluded.equipment, inventory=excluded.inventory,
                    cultivation_direction=excluded.cultivation_direction, active_streak=excluded.active_streak,
                    name_changed_at=excluded.name_changed_at, char_level=excluded.char_level, season=excluded.season,
                    last_active_time=excluded.last_active_time,
                    hairstyle=excluded.hairstyle, outfit=excluded.outfit, accessory=excluded.accessory,
                    overflow_protected=excluded.overflow_protected
                """)) {
            ps.setString(1, p.userId());
            ps.setString(2, p.groupId());
            ps.setString(3, p.name());
            ps.setString(4, p.gender() == null ? null : p.gender().label());
            ps.setString(5, p.realm().label());
            ps.setLong(6, p.exp());
            ps.setLong(7, p.spiritStones());
            ps.setString(8, p.equipment());
            ps.setString(9, p.inventory());
            ps.setString(10, p.direction() == null ? null : p.direction().label());
            ps.setInt(11, p.activeStreak());
            ps.setLong(12, p.nameChangedAt());
            ps.setInt(13, p.level());
            ps.setInt(14, p.season());
            ps.setLong(15, p.createdAt());
            ps.setLong(16, p.lastActiveTime());
            ps.setString(17, p.hairstyle());
            ps.setString(18, p.outfit());
            ps.setString(19, p.accessory());
            ps.setInt(20, p.overflowProtected() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("保存玩家失败", e);
        }
    }
}
