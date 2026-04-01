# JDBC SQL 执行工具类实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 JdbcUtils 工具类和 SqlSafetyUtils 安全检查工具类

**Architecture:**
- `SqlSafetyUtils`：静态工具类，负责检查 SQL 是否安全可执行
- `JdbcUtils`：Spring `@Component`，使用 JdbcTemplate 参数化查询防注入，内部调用 SqlSafetyUtils 检查
- 使用 Jackson ObjectMapper 将结果序列化为 JSON

**Tech Stack:** Spring Boot, Spring JDBC (JdbcTemplate), Jackson

---

## 文件结构

| 文件 | 说明 |
|------|------|
| Create: `src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/SqlSafetyUtils.java` | SQL 安全检查工具类 |
| Create: `src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtils.java` | JDBC 执行工具类 |
| Create: `src/test/java/com/gk/llm/mcp/mysqlservermcp/utils/SqlSafetyUtilsTest.java` | SqlSafetyUtils 单元测试 |
| Create: `src/test/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtilsTest.java` | JdbcUtils 单元测试 |

---

## Task 1: SqlSafetyUtils 实现

### 验收标准
- `isSafe("SELECT * FROM user")` → true
- `isSafe("SELECT * FROM user WHERE id = ?")` → true
- `isSafe("UPDATE user SET name = ? WHERE id = ?")` → true
- `isSafe("UPDATE user SET name = ?")` → false（无 WHERE）
- `isSafe("DELETE FROM user WHERE id = ?")` → true
- `isSafe("DELETE FROM user")` → false（无 WHERE）
- `isSafe("CREATE TABLE test (id INT)")` → true
- `isSafe("DROP TABLE user")` → false
- `isSafe("TRUNCATE TABLE user")` → false
- `isSafe("DELETE FROM user WHERE id = 1")` → true（带 WHERE）

### Steps

- [ ] **Step 1: 创建测试文件 SqlSafetyUtilsTest.java**

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=SqlSafetyUtilsTest`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 创建 SqlSafetyUtils 实现**

```java
package com.gk.llm.mcp.mysqlservermcp.utils;

import java.util.regex.Pattern;

/**
 * SQL 安全检查工具类
 * 检查 SQL 是否在允许的安全范围内执行
 */
public class SqlSafetyUtils {

    // UPDATE 必须包含 WHERE 子句
    private static final Pattern UPDATE_NO_WHERE = Pattern.compile(
        "^\\s*UPDATE\\s+\\w+\\s+SET\\s+", Pattern.CASE_INSENSITIVE);

    // DELETE 必须包含 WHERE 子句（忽略末尾分号和空格）
    private static final Pattern DELETE_NO_WHERE = Pattern.compile(
        "^\\s*DELETE\\s+FROM\\s+\\w+\\s*(;|\\s*$)", Pattern.CASE_INSENSITIVE);

    // 危险 DDL：DROP
    private static final Pattern DROP_PATTERN = Pattern.compile(
        "^\\s*DROP\\s+", Pattern.CASE_INSENSITIVE);

    // 危险 DDL：TRUNCATE
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile(
        "^\\s*TRUNCATE\\s+", Pattern.CASE_INSENSITIVE);

    // 危险 DDL：GRANT/REVOKE
    private static final Pattern GRANT_REVOKE_PATTERN = Pattern.compile(
        "^\\s*(GRANT|REVOKE)\\s+", Pattern.CASE_INSENSITIVE);

    /**
     * 检查 SQL 是否安全可执行
     * @param sql SQL 语句
     * @return true=安全可执行，false=危险拒绝执行
     */
    public static boolean isSafe(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return true;
        }
        String trimmed = sql.trim();

        // 允许：SELECT
        if (trimmed.toUpperCase().startsWith("SELECT")) {
            return true;
        }

        // 允许：带 WHERE 的 UPDATE
        if (UPDATE_NO_WHERE.matcher(trimmed).matches()) {
            return false;
        }

        // 允许：带 WHERE 的 DELETE
        if (DELETE_NO_WHERE.matcher(trimmed).matches()) {
            return false;
        }

        // 允许：CREATE TABLE/INDEX
        if (trimmed.toUpperCase().startsWith("CREATE")) {
            return true;
        }

        // 允许：DESCRIBE/SHOW
        if (trimmed.toUpperCase().startsWith("DESCRIBE") ||
            trimmed.toUpperCase().startsWith("SHOW")) {
            return true;
        }

        // 允许：ALTER TABLE
        if (trimmed.toUpperCase().startsWith("ALTER")) {
            return true;
        }

        // 拒绝：DROP
        if (DROP_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // 拒绝：TRUNCATE
        if (TRUNCATE_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // 拒绝：GRANT/REVOKE
        if (GRANT_REVOKE_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // 其他未分类的默认安全（INSERT 等）
        return true;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=SqlSafetyUtilsTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/SqlSafetyUtils.java
git add src/test/java/com/gk/llm/mcp/mysqlservermcp/utils/SqlSafetyUtilsTest.java
git commit -m "$(cat <<'EOF'
feat: 添加 SqlSafetyUtils SQL 安全检查工具类

- 实现 isSafe() 方法检查 SQL 是否危险
- SELECT 任意、UPDATE/DELETE 必须带 WHERE
- 拒绝 DROP/TRUNCATE/GRANT/REVOKE 等危险操作

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: JdbcUtils 实现

### 验收标准
- `executeQuery(sql, args)` 返回 JSON 格式查询结果
- `executeUpdate(sql, args)` 返回 JSON 格式影响行数
- `executeDDL(sql)` 返回 JSON 格式执行结果
- 危险 SQL 直接返回拒绝 JSON，不执行
- 异常包装为 JSON 错误格式

### Steps

- [ ] **Step 1: 创建 JdbcUtilsTest 测试文件（使用 H2 内存数据库）**

```java
package com.gk.llm.mcp.mysqlservermcp.utils;

import com.gk.llm.mcp.mysqlservermcp.config.DataSourceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JdbcUtilsTest {

    private JdbcUtils jdbcUtils;

    @BeforeEach
    void setUp() {
        // 使用 H2 内存数据库测试
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        String username = "sa";
        String password = "";
        jdbcUtils = new JdbcUtils(url, username, password);

        // 创建测试表
        jdbcUtils.executeDDL("CREATE TABLE user (id INT PRIMARY KEY, name VARCHAR(100))");
    }

    @Test
    void testExecuteQuery() {
        jdbcUtils.executeUpdate("INSERT INTO user VALUES (1, 'zhangsan')");
        jdbcUtils.executeUpdate("INSERT INTO user VALUES (2, 'lisi')");

        String result = jdbcUtils.executeQuery("SELECT * FROM user WHERE id = ?", 1);

        assertTrue(result.contains("\"success\":true"));
        assertTrue(result.contains("\"rowCount\":1"));
        assertTrue(result.contains("zhangsan"));
    }

    @Test
    void testExecuteUpdate() {
        jdbcUtils.executeUpdate("INSERT INTO user VALUES (1, 'zhangsan')");

        String result = jdbcUtils.executeUpdate("UPDATE user SET name = ? WHERE id = ?", "newname", 1);

        assertTrue(result.contains("\"success\":true"));
        assertTrue(result.contains("\"affectedRows\":1"));
    }

    @Test
    void testExecuteDDL() {
        String result = jdbcUtils.executeDDL("CREATE TABLE product (id INT PRIMARY KEY)");

        assertTrue(result.contains("\"success\":true"));
        assertTrue(result.contains("\"message\":\"执行成功\""));
    }

    @Test
    void testDangerousSqlRejected() {
        String result = jdbcUtils.executeDDL("DROP TABLE user");

        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("危险SQL"));
    }

    @Test
    void testUpdateWithoutWhereRejected() {
        String result = jdbcUtils.executeUpdate("UPDATE user SET name = 'test'");

        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("危险SQL"));
    }

    @Test
    void testDeleteWithoutWhereRejected() {
        String result = jdbcUtils.executeUpdate("DELETE FROM user");

        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("危险SQL"));
    }

    @Test
    void testSqlErrorHandling() {
        String result = jdbcUtils.executeQuery("SELECT * FROM nonexistent");

        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("\"error\""));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=JdbcUtilsTest`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 添加 H2 测试依赖到 pom.xml**

查看 pom.xml 确认 H2 scope 是 test，但 JdbcUtils 在运行时需要 H2。需要在 dependencies 中添加：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>compile</scope>
</dependency>
```

- [ ] **Step 4: 创建 JdbcUtils 实现**

```java
package com.gk.llm.mcp.mysqlservermcp.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * JDBC SQL 执行工具类
 * 使用 JdbcTemplate 参数化查询防止 SQL 注入
 */
@Component
public class JdbcUtils {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcUtils(@Value("${spring.datasource.url}") String url,
                     @Value("${spring.datasource.username}") String username,
                     @Value("${spring.datasource.password}") String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // 供测试使用的构造函数
    JdbcUtils(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 执行 SELECT 查询（参数化，防止注入）
     * @param sql 带占位符的 SQL
     * @param args 参数值
     * @return JSON 格式结果
     */
    public String executeQuery(String sql, Object... args) {
        // 安全检查
        if (!SqlSafetyUtils.isSafe(sql)) {
            return dangerJson(sql);
        }

        try {
            ArrayNode rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
                ObjectNode row = objectMapper.createObjectNode();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    if (value != null) {
                        row.put(columnName, value.toString());
                    } else {
                        row.putNull(columnName);
                    }
                }
                return row;
            }, args);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.set("data", objectMapper.valueToTree(rows));
            result.put("rowCount", rows.size());
            return objectMapper.writeValueAsString(result);

        } catch (SQLException e) {
            return errorJson(e.getMessage());
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    /**
     * 执行 INSERT/UPDATE/DELETE（参数化，防止注入）
     * @param sql 带占位符的 SQL
     * @param args 参数值
     * @return JSON 格式结果
     */
    public String executeUpdate(String sql, Object... args) {
        // 安全检查
        if (!SqlSafetyUtils.isSafe(sql)) {
            return dangerJson(sql);
        }

        try {
            int affectedRows = jdbcTemplate.update(sql, args);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("affectedRows", affectedRows);
            return objectMapper.writeValueAsString(result);

        } catch (SQLException e) {
            return errorJson(e.getMessage());
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    /**
     * 执行 DDL（CREATE、ALTER 等）
     * @param sql DDL 语句
     * @return JSON 格式结果
     */
    public String executeDDL(String sql) {
        // 安全检查
        if (!SqlSafetyUtils.isSafe(sql)) {
            return dangerJson(sql);
        }

        try {
            jdbcTemplate.execute(sql);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("message", "执行成功");
            return objectMapper.writeValueAsString(result);

        } catch (SQLException e) {
            return errorJson(e.getMessage());
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private String dangerJson(String sql) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("error", "危险SQL，已拒绝执行: " + sql);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"危险SQL，已拒绝执行\"}";
        }
    }

    private String errorJson(String error) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            result.put("error", error);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"未知错误\"}";
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn test -Dtest=JdbcUtilsTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtils.java
git add src/test/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtilsTest.java
git add pom.xml
git commit -m "$(cat <<'EOF'
feat: 添加 JdbcUtils SQL 执行工具类

- 使用 JdbcTemplate 参数化查询防止 SQL 注入
- executeQuery/executeUpdate/executeDDL 方法
- 内部调用 SqlSafetyUtils 进行安全检查
- 返回 JSON 格式结果

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## 执行方式

**Plan complete and saved to `docs/superpowers/plans/2026-04-01-jdbc-sql-execute-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
