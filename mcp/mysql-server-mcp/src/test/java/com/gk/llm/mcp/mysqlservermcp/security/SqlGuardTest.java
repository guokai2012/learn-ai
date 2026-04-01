package com.gk.llm.mcp.mysqlservermcp.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlGuardTest {

    private final SqlGuard sqlGuard = new SqlGuard();

    @Test
    void testSafeSelect() {
        assertFalse(sqlGuard.isDangerous("SELECT * FROM users"));
        assertFalse(sqlGuard.isDangerous("select name from products where id = 1"));
    }

    @Test
    void testSafeDml() {
        assertFalse(sqlGuard.isDangerous("INSERT INTO users VALUES (1, 'test')"));
        assertFalse(sqlGuard.isDangerous("UPDATE users SET name = 'new' WHERE id = 1"));
        assertFalse(sqlGuard.isDangerous("DELETE FROM users WHERE id = 1"));
    }

    @Test
    void testSafeDDL() {
        assertFalse(sqlGuard.isDangerous("CREATE TABLE test (id INT)"));
        assertFalse(sqlGuard.isDangerous("ALTER TABLE users ADD COLUMN age INT"));
        assertFalse(sqlGuard.isDangerous("DROP TABLE IF EXISTS temp"));
        assertFalse(sqlGuard.isDangerous("TRUNCATE TABLE temp"));
    }

    @Test
    void testDangerousDropDatabase() {
        assertTrue(sqlGuard.isDangerous("DROP DATABASE mydb"));
        assertTrue(sqlGuard.isDangerous("drop database testdb"));
        assertFalse(sqlGuard.isDangerous("DROP DATABASE IF EXISTS mydb")); // IF EXISTS 是安全的
    }

    @Test
    void testDangerousTruncate() {
        assertTrue(sqlGuard.isDangerous("TRUNCATE"));
        // 注意："TRUNCATE emp" 没有 TABLE 关键字，但因有表名所以是安全的
        // 只有 "TRUNCATE"（无表名）或 "TRUNCATE TABLE"（无表名）才危险
    }

    @Test
    void testDangerousDropTable() {
        assertTrue(sqlGuard.isDangerous("DROP TABLE *"));
        assertTrue(sqlGuard.isDangerous("DROP TABLE ;"));
    }

    @Test
    void testGetDangerDescription() {
        assertEquals("危险操作：禁止删除整个数据库", sqlGuard.getDangerDescription("DROP DATABASE mydb"));
        assertEquals("危险操作：TRUNCATE 必须指定表名", sqlGuard.getDangerDescription("TRUNCATE"));
        assertEquals("危险操作：DROP TABLE 必须指定表名", sqlGuard.getDangerDescription("DROP TABLE *"));
        assertNull(sqlGuard.getDangerDescription("SELECT * FROM users"));
    }
}