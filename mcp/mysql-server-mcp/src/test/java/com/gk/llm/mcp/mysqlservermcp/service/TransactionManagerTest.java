package com.gk.llm.mcp.mysqlservermcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class TransactionManagerTest {

    private DataSource ds;
    private TransactionManager tm;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 为每个测试创建新的嵌入式数据库实例
        ds = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb-" + System.nanoTime()) // 使用唯一名称确保隔离
            .build();

        tm = new TransactionManager(ds);
        jdbcTemplate = new JdbcTemplate(ds);
    }

    @Test
    void testCommitOnSuccess() {
        // 创建表
        jdbcTemplate.execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(100))");

        // 执行成功的事务
        tm.execute(() -> {
            jdbcTemplate.execute("INSERT INTO test VALUES (1, 'Alice')");
            jdbcTemplate.execute("INSERT INTO test VALUES (2, 'Bob')");
            return null;
        });

        // 验证数据已提交
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test", Integer.class);
        assertEquals(2, count);
    }

    @Test
    void testRollbackOnFailure() {
        // 创建表
        jdbcTemplate.execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(100))");

        // 尝试执行会失败的事务
        assertThrows(RuntimeException.class, () -> {
            tm.execute(() -> {
                jdbcTemplate.execute("INSERT INTO test VALUES (1, 'Alice')");
                jdbcTemplate.execute("INSERT INTO test VALUES (1, 'Bob')"); // 违反主键
                return null;
            });
        });

        // 验证数据已回滚（表为空）
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test", Integer.class);
        assertEquals(0, count);
    }
}
