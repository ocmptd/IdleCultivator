package com.ocmptd.idlecultivator.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 连接与建表。
 */
public class Database implements AutoCloseable {
    private final Connection connection;

    public Database(String path) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT PRIMARY KEY,
                        group_id TEXT,
                        name TEXT,
                        gender TEXT,
                        level TEXT NOT NULL DEFAULT '练气期',
                        current_exp INTEGER NOT NULL DEFAULT 0,
                        spirit_stones INTEGER NOT NULL DEFAULT 0,
                        equipment TEXT NOT NULL DEFAULT '',
                        inventory TEXT NOT NULL DEFAULT '',
                        cultivation_direction TEXT,
                        active_streak INTEGER NOT NULL DEFAULT 0,
                        name_changed_at INTEGER NOT NULL DEFAULT 0,
                        char_level INTEGER NOT NULL DEFAULT 0,
                        season INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL
                    )""");
            migrate(st);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS cultivation_tasks (
                        task_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id TEXT NOT NULL,
                        group_id TEXT,
                        method TEXT,
                        start_time INTEGER NOT NULL,
                        duration_minutes INTEGER NOT NULL,
                        status INTEGER NOT NULL DEFAULT 0,
                        expected_reward INTEGER NOT NULL DEFAULT 0,
                        overflow_penalty REAL NOT NULL DEFAULT 0
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pending_notices (
                        notice_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS group_status (
                        group_id TEXT PRIMARY KEY,
                        last_active_time INTEGER,
                        message_count INTEGER NOT NULL DEFAULT 0,
                        cultivation_bonus REAL NOT NULL DEFAULT 1.0,
                        next_bonus_time INTEGER,
                        activity_level INTEGER NOT NULL DEFAULT 1,
                        hourly_msg_count INTEGER NOT NULL DEFAULT 0,
                        tide_until INTEGER NOT NULL DEFAULT 0,
                        last_tide_date TEXT
                    )""");
        }
    }

    /** 对已存在的旧库补充新列。 */
    private void migrate(Statement st) throws SQLException {
        addColumnIfMissing(st, "users", "name_changed_at INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "users", "char_level INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "users", "season INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(st, "users", "last_active_time INTEGER NOT NULL DEFAULT 0");
        // Phase 2: 形象定制字段
        addColumnIfMissing(st, "users", "hairstyle TEXT");
        addColumnIfMissing(st, "users", "outfit TEXT");
        addColumnIfMissing(st, "users", "accessory TEXT");
        addColumnIfMissing(st, "users", "overflow_protected INTEGER NOT NULL DEFAULT 0");
        // group_status 增列:活跃度等级、每小时消息计数、灵气潮汐截止、上次潮汐日期
        addColumnIfMissing(st, "group_status", "activity_level INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(st, "group_status", "hourly_msg_count INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "group_status", "tide_until INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "group_status", "last_tide_date TEXT");
        createGroupBeastsTable(st);
        createMutualHelpsTable(st);
        createDaoCompanionsTable(st);
        createSectWarWeeklyTable(st);
    }

    private void addColumnIfMissing(Statement st, String table, String columnDef) {
        try {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + columnDef);
        } catch (SQLException ignored) {
            // 列已存在
        }
    }

    private void createGroupBeastsTable(Statement st) throws SQLException {
        st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS group_beasts (
                    group_id TEXT PRIMARY KEY,
                    beast_name TEXT NOT NULL,
                    hp INTEGER NOT NULL,
                    max_hp INTEGER NOT NULL,
                    exp_reward INTEGER NOT NULL,
                    stone_reward INTEGER NOT NULL,
                    spawn_time INTEGER NOT NULL
                )""");
    }

    private void createMutualHelpsTable(Statement st) throws SQLException {
        st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mutual_helps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    target_id TEXT NOT NULL,
                    group_id TEXT,
                    expire_time INTEGER NOT NULL
                )""");
    }

    private void createDaoCompanionsTable(Statement st) throws SQLException {
        st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dao_companions (
                    user_id TEXT PRIMARY KEY,
                    partner_id TEXT NOT NULL,
                    group_id TEXT,
                    established_at INTEGER NOT NULL
                )""");
    }

    private void createSectWarWeeklyTable(Statement st) throws SQLException {
        st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sect_war_weekly (
                    week_key TEXT NOT NULL,
                    group_id TEXT NOT NULL,
                    total_exp INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (week_key, group_id)
                )""");
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
