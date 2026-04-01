package com.gk.llm.mcp.mysqlservermcp.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlSafetyUtilsTest {

    @Test
    void testSelectAlwaysSafe() {
        assertTrue(SqlSafetyUtils.isSafe("SELECT * FROM user"));
        assertTrue(SqlSafetyUtils.isSafe("select name from products"));
        assertTrue(SqlSafetyUtils.isSafe("SELECT id, name FROM user WHERE id = ?"));
    }

    @Test
    void testUpdateRequiresWhere() {
        assertTrue(SqlSafetyUtils.isSafe("UPDATE user SET name = ? WHERE id = ?"));
        assertFalse(SqlSafetyUtils.isSafe("UPDATE user SET name = ?"));
        assertFalse(SqlSafetyUtils.isSafe("UPDATE user SET name = 'test'"));
    }

    @Test
    void testDeleteRequiresWhere() {
        assertTrue(SqlSafetyUtils.isSafe("DELETE FROM user WHERE id = ?"));
        assertFalse(SqlSafetyUtils.isSafe("DELETE FROM user"));
        assertTrue(SqlSafetyUtils.isSafe("DELETE FROM user WHERE id = 1"));
        // 带 WHERE 的才安全
    }

    @Test
    void testDDLAllowed() {
        assertTrue(SqlSafetyUtils.isSafe("CREATE TABLE test (id INT)"));
        assertTrue(SqlSafetyUtils.isSafe("CREATE INDEX idx ON user(name)"));
        assertTrue(SqlSafetyUtils.isSafe("ALTER TABLE user ADD COLUMN age INT"));
        assertTrue(SqlSafetyUtils.isSafe("DESCRIBE user"));
        assertTrue(SqlSafetyUtils.isSafe("SHOW TABLES"));
    }

    @Test
    void testDDLForbidden() {
        assertFalse(SqlSafetyUtils.isSafe("DROP TABLE user"));
        assertFalse(SqlSafetyUtils.isSafe("DROP DATABASE mydb"));
        assertFalse(SqlSafetyUtils.isSafe("TRUNCATE TABLE user"));
        assertFalse(SqlSafetyUtils.isSafe("GRANT ALL ON user TO admin"));
        assertFalse(SqlSafetyUtils.isSafe("REVOKE ALL ON user FROM admin"));
    }

    @Test
    void testNullOrEmpty() {
        assertTrue(SqlSafetyUtils.isSafe(null));
        assertTrue(SqlSafetyUtils.isSafe(""));
        assertTrue(SqlSafetyUtils.isSafe("   "));
    }
}