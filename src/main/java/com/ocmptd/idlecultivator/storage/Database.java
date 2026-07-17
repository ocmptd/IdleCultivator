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
                        next_bonus_time INTEGER
                    )""");
        }
    }

    /** 对已存在的旧库补充新列。 */
    private void migrate(Statement st) {
        addColumnIfMissing(st, "name_changed_at INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "char_level INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(st, "season INTEGER NOT NULL DEFAULT 1");
    }

    private void addColumnIfMissing(Statement st, String columnDef) {
        try {
            st.executeUpdate("ALTER TABLE users ADD COLUMN " + columnDef);
        } catch (SQLException ignored) {
            // 列已存在
        }
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
