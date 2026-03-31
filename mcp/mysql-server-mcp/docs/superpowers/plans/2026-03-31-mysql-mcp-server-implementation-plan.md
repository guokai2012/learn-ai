# MySQL MCP Server 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 构建一个基于 Spring AI 的 MySQL MCP Server，支持 DQL/DML/DDL 执行、自动事务管理和危险语句拦截。

**架构概述：**
- 使用 Spring AI MCP Server Starter，通过 STDIO 协议与 AI 客户端通信
- 三个 MCP Tool 分别处理 DQL、DML、DDL 操作
- 自动事务管理器包装多个操作，确保原子性
- SQL 守卫器拦截危险语句

**技术栈：** Spring Boot 4.0.5, Spring AI 2.0.0-M4, MySQL Connector/J, Lombok

---

## 文件结构

```
src/main/java/com/gk/llm/mcp/mysqlservermcp/
├── MysqlServerMcpApplication.java           # Spring Boot 入口（已存在）
├── MysqlMcpServerAutoConfiguration.java       # MCP Server 自动配置【新】
├── tool/                                      # MCP Tool 实现【新目录】
│   ├── ExecuteQueryTool.java                 # DQL 执行器
│   ├── ExecuteUpdateTool.java               # DML 执行器
│   └── ExecuteDDlTool.java                  # DDL 执行器
├── service/                                   # 服务层【新目录】
│   └── TransactionManager.java              # 自动事务管理
├── security/                                 # 安全层【新目录】
│   └── SqlGuard.java                        # 危险语句拦截器
└── config/                                   # 配置层【新目录】
    └── DataSourceProperties.java            # 数据源配置属性

src/main/resources/
├── application.properties                    # 应用配置【新】
└── META-INF/
    └── spring.factories                     # 自动配置【新】
```

---

## Task 1: 项目依赖配置

**文件：**
- 修改: `pom.xml`

- [ ] **Step 1: 添加 MySQL 依赖**

确认 pom.xml 中已有 `spring-ai-starter-mcp-server`（已存在）和 `lombok`（已存在）。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: 添加 Lombok 依赖**

Lombok 已存在于 pom.xml 的 devDependencies 中，确认 scope 为 `optional`

- [ ] **Step 3: 提交**

```bash
git add pom.xml && git commit -m "deps: 添加 MySQL Connector 依赖"
```

---

## Task 2: 数据源配置

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/config/DataSourceProperties.java`
- 创建: `src/main/resources/application.properties`
- 创建: `src/main/resources/META-INF/spring.factories`

- [ ] **Step 1: 创建 DataSourceProperties 配置类**

```java
package com.gk.llm.mcp.mysqlservermcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {
    private String url = "jdbc:mysql://localhost:3306/test";
    private String username = "root";
    private String password;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 2: 创建 application.properties**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

- [ ] **Step 3: 创建 spring.factories 自动配置**

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.gk.llm.mcp.mysqlservermcp.MysqlMcpServerAutoConfiguration
```

- [ ] **Step 4: 提交**

```bash
git add . && git commit -m "feat: 添加数据源配置"
```

---

## Task 3: SQL 危险语句拦截器

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/security/SqlGuard.java`

- [ ] **Step 1: 创建 SqlGuard 危险语句拦截器**

```java
package com.gk.llm.mcp.mysqlservermcp.security;

import java.util.regex.Pattern;

/**
 * SQL 危险语句拦截器
 * 禁止执行高危 DDL 操作
 */
public class SqlGuard {

    // 拦截：DROP DATABASE 不带 IF EXISTS（IF EXISTS 是安全的）
    private static final Pattern DROP_DATABASE_PATTERN =
        Pattern.compile("^\\s*DROP\\s+DATABASE\\s+(?!IF\\s+EXISTS)\\w+\\s*$", Pattern.CASE_INSENSITIVE);

    // 拦截：TRUNCATE 不带 TABLE 关键字
    private static final Pattern TRUNCATE_NO_TABLE_PATTERN =
        Pattern.compile("^\\s*TRUNCATE\\s+TABLE?\\s*$", Pattern.CASE_INSENSITIVE);

    // 拦截：DROP TABLE * 或 DROP TABLE 无具体表名
    private static final Pattern DROP_TABLE_NO_NAME_PATTERN =
        Pattern.compile("^\\s*DROP\\s+TABLE\\s+(\\*|;)\\s*$", Pattern.CASE_INSENSITIVE);

    /**
     * 检查 SQL 是否为危险语句
     * @param sql SQL 语句
     * @return true 如果语句危险应被拦截
     */
    public boolean isDangerous(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmed = sql.trim();
        return DROP_DATABASE_PATTERN.matcher(trimmed).matches()
            || TRUNCATE_NO_TABLE_PATTERN.matcher(trimmed).matches()
            || DROP_TABLE_NO_NAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 获取危险语句描述
     * @param sql SQL 语句
     * @return 危险描述，如果安全则返回 null
     */
    public String getDangerDescription(String sql) {
        if (isDangerous(sql)) {
            if (DROP_DATABASE_PATTERN.matcher(sql.trim()).matches()) {
                return "危险操作：禁止删除整个数据库";
            }
            if (TRUNCATE_NO_TABLE_PATTERN.matcher(sql.trim()).matches()) {
                return "危险操作：TRUNCATE 必须指定表名";
            }
            if (DROP_TABLE_NO_NAME_PATTERN.matcher(sql.trim()).matches()) {
                return "危险操作：DROP TABLE 必须指定表名";
            }
            return "危险操作：未知类型";
        }
        return null;
    }
}
```

- [ ] **Step 2: 编写单元测试**

创建: `src/test/java/com/gk/llm/mcp/mysqlservermcp/security/SqlGuardTest.java`

```java
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
```

- [ ] **Step 3: 运行测试验证**

```bash
./mvnw test -Dtest=SqlGuardTest
```

预期：所有测试通过

- [ ] **Step 4: 提交**

```bash
git add . && git commit -m "feat: 添加 SQL 危险语句拦截器"
```

---

## Task 4: 事务管理器

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/service/TransactionManager.java`

- [ ] **Step 1: 创建 TransactionManager**

```java
package com.gk.llm.mcp.mysqlservermcp.service;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 自动事务管理器
 * 用于包装多个数据库操作，确保原子性
 */
public class TransactionManager {

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 执行带事务的回调
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T execute(TransactionCallback<T> action) {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
        TransactionStatus status = txManager.getTransaction(txDefinition);

        try {
            T result = action.doInTransaction();
            txManager.commit(status);
            return result;
        } catch (Exception e) {
            txManager.rollback(status);
            throw new RuntimeException("事务执行失败，已回滚: " + e.getMessage(), e);
        }
    }

    /**
     * 事务回调接口
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction() throws SQLException;
    }
}
```

- [ ] **Step 2: 编写单元测试**

创建: `src/test/java/com/gk/llm/mcp/mysqlservermcp/service/TransactionManagerTest.java`

```java
package com.gk.llm.mcp.mysqlservermcp.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class TransactionManagerTest {

    @Test
    void testCommitOnSuccess() throws Exception {
        DataSource ds = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();

        TransactionManager tm = new TransactionManager(ds);

        // 创建表
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(100))");
        }

        // 执行成功的事务
        tm.execute(() -> {
            try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
                s.execute("INSERT INTO test VALUES (1, 'Alice')");
                s.execute("INSERT INTO test VALUES (2, 'Bob')");
            }
            return null;
        });

        // 验证数据已提交
        try (Connection c = ds.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM test")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void testRollbackOnFailure() {
        DataSource ds = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();

        TransactionManager tm = new TransactionManager(ds);

        // 创建表
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(100))");
        }

        // 尝试执行会失败的事务
        assertThrows(RuntimeException.class, () -> {
            tm.execute(() -> {
                try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
                    s.execute("INSERT INTO test VALUES (1, 'Alice')");
                    s.execute("INSERT INTO test VALUES (1, 'Bob')"); // 违反主键
                }
                return null;
            });
        });

        // 验证数据已回滚（表为空）
        try (Connection c = ds.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM test")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}
```

- [ ] **Step 3: 运行测试验证**

```bash
./mvnw test -Dtest=TransactionManagerTest
```

预期：所有测试通过

- [ ] **Step 4: 提交**

```bash
git add . && git commit -m "feat: 添加事务管理器"
```

---

## Task 5: DQL 执行器 (SELECT)

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/tool/ExecuteQueryTool.java`

- [ ] **Step 1: 创建 ExecuteQueryTool**

```java
package com.gk.llm.mcp.mysqlservermcp.tool;

import com.gk.llm.mcp.mysqlservermcp.service.TransactionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

/**
 * DQL 执行器 - 处理 SELECT 查询
 * 使用只读事务
 */
@Component
public class ExecuteQueryTool {

    private final DataSource dataSource;
    private final TransactionManager transactionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecuteQueryTool(DataSource dataSource, TransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Tool(description = "执行 SELECT 查询，返回查询结果")
    public String executeQuery(@ToolParam(description = "SQL SELECT 语句") String sql) {
        // 简单的查询验证
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            return "错误：只支持 SELECT 查询，请使用其他工具执行 INSERT/UPDATE/DELETE";
        }

        try {
            ArrayNode resultArray = transactionManager.execute(() -> {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    ArrayNode rows = objectMapper.createArrayNode();

                    while (rs.next()) {
                        ObjectNode row = objectMapper.createObjectNode();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = rs.getObject(i);
                            if (value != null) {
                                row.put(columnName, value.toString());
                            } else {
                                row.putNull(columnName);
                            }
                        }
                        rows.add(row);
                    }
                    return rows;
                }
            });

            ObjectNode result = objectMapper.createObjectNode();
            result.set("data", resultArray);
            result.put("rowCount", resultArray.size());
            return objectMapper.writeValueAsString(result);

        } catch (SQLException e) {
            return "数据库错误：" + e.getMessage();
        } catch (RuntimeException e) {
            return "事务错误：" + e.getMessage();
        } catch (Exception e) {
            return "序列化错误：" + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add . && git commit -m "feat: 添加 DQL 执行器"
```

---

## Task 6: DML 执行器 (INSERT/UPDATE/DELETE)

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/tool/ExecuteUpdateTool.java`

- [ ] **Step 1: 创建 ExecuteUpdateTool**

```java
package com.gk.llm.mcp.mysqlservermcp.tool;

import com.gk.llm.mcp.mysqlservermcp.service.TransactionManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

/**
 * DML 执行器 - 处理 INSERT、UPDATE、DELETE
 */
@Component
public class ExecuteUpdateTool {

    private final DataSource dataSource;
    private final TransactionManager transactionManager;

    public ExecuteUpdateTool(DataSource dataSource, TransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Tool(description = "执行 INSERT、UPDATE、DELETE 语句")
    public String executeUpdate(@ToolParam(description = "SQL INSERT、UPDATE 或 DELETE 语句") String sql) {
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("INSERT") && !upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE")) {
            return "错误：只支持 INSERT/UPDATE/DELETE，请使用其他工具执行 SELECT 或 DDL";
        }

        try {
            int affectedRows = transactionManager.execute(() -> {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    return stmt.executeUpdate(sql);
                }
            });

            return "成功，影响行数：" + affectedRows;

        } catch (SQLException e) {
            return "数据库错误：" + e.getMessage();
        } catch (RuntimeException e) {
            return "事务错误：" + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add . && git commit -m "feat: 添加 DML 执行器"
```

---

## Task 7: DDL 执行器 (CREATE/ALTER/DROP/TRUNCATE)

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/tool/ExecuteDDlTool.java`

- [ ] **Step 1: 创建 ExecuteDDlTool**

```java
package com.gk.llm.mcp.mysqlservermcp.tool;

import com.gk.llm.mcp.mysqlservermcp.security.SqlGuard;
import com.gk.llm.mcp.mysqlservermcp.service.TransactionManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DDL 执行器 - 处理 CREATE、ALTER、DROP、TRUNCATE 等
 * 包含危险语句拦截
 */
@Component
public class ExecuteDDlTool {

    private final DataSource dataSource;
    private final TransactionManager transactionManager;
    private final SqlGuard sqlGuard;

    public ExecuteDDlTool(DataSource dataSource,
                         TransactionManager transactionManager,
                         SqlGuard sqlGuard) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.sqlGuard = sqlGuard;
    }

    @Tool(description = "执行 CREATE、ALTER、DROP、TRUNCATE 等 DDL 语句")
    public String executeDDl(@ToolParam(description = "SQL DDL 语句") String sql) {
        // 危险语句检查
        String dangerDesc = sqlGuard.getDangerDescription(sql);
        if (dangerDesc != null) {
            return "拦截：" + dangerDesc + "，SQL：" + sql;
        }

        try {
            transactionManager.execute(() -> {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                return null;
            });

            return "成功执行 DDL：" + sql;

        } catch (SQLException e) {
            return "数据库错误：" + e.getMessage();
        } catch (RuntimeException e) {
            return "事务错误：" + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add . && git commit -m "feat: 添加 DDL 执行器"
```

---

## Task 8: MCP Server 自动配置

**文件：**
- 创建: `src/main/java/com/gk/llm/mcp/mysqlservermcp/MysqlMcpServerAutoConfiguration.java`

- [ ] **Step 1: 创建 MCP Server 自动配置类**

```java
package com.gk.llm.mcp.mysqlservermcp;

import com.gk.llm.mcp.mysqlservermcp.config.DataSourceProperties;
import com.gk.llm.mcp.mysqlservermcp.security.SqlGuard;
import com.gk.llm.mcp.mysqlservermcp.service.TransactionManager;
import com.gk.llm.mcp.mysqlservermcp.tool.ExecuteDDlTool;
import com.gk.llm.mcp.mysqlservermcp.tool.ExecuteQueryTool;
import com.gk.llm.mcp.mysqlservermcp.tool.ExecuteUpdateTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MCP Server 自动配置
 */
@Configuration
@EnableAutoConfiguration
public class MysqlMcpServerAutoConfiguration {

    // 提供一个简单的 DataSource Bean，供 TransactionManager 使用
    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return new org.apache.commons.dbcp2.BasicDataSource() {{
            setUrl(properties.getUrl());
            setUsername(properties.getUsername());
            setPassword(properties.getPassword());
            setDriverClassName("com.mysql.cj.jdbc.Driver");
        }};
    }

    @Bean
    public SqlGuard sqlGuard() {
        return new SqlGuard();
    }

    @Bean
    public TransactionManager transactionManager(DataSource dataSource) {
        return new TransactionManager(dataSource);
    }

    @Bean
    public ExecuteQueryTool executeQueryTool(DataSource dataSource, TransactionManager transactionManager) {
        return new ExecuteQueryTool(dataSource, transactionManager);
    }

    @Bean
    public ExecuteUpdateTool executeUpdateTool(DataSource dataSource,
                                                TransactionManager transactionManager) {
        return new ExecuteUpdateTool(dataSource, transactionManager);
    }

    @Bean
    public ExecuteDDlTool executeDDlTool(DataSource dataSource,
                                          TransactionManager transactionManager,
                                          SqlGuard sqlGuard) {
        return new ExecuteDDlTool(dataSource, transactionManager, sqlGuard);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ExecuteQueryTool executeQueryTool,
                                                     ExecuteUpdateTool executeUpdateTool,
                                                     ExecuteDDlTool executeDDlTool) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(executeQueryTool, executeUpdateTool, executeDDlTool)
            .build();
    }
}
```

- [ ] **Step 2: 更新 spring.factories**

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.gk.llm.mcp.mysqlservermcp.MysqlMcpServerAutoConfiguration
```

- [ ] **Step 3: 添加 Apache Commons DBCP2 依赖到 pom.xml**

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
</dependency>
```

- [ ] **Step 4: 提交**

```bash
git add . && git commit -m "feat: 添加 MCP Server 自动配置"
```

---

## Task 9: STDIO 传输配置

**文件：**
- 创建: `src/main/resources/application.properties`（如果尚未创建）

- [ ] **Step 1: 配置 STDIO 传输**

在 `application.properties` 中添加：

```properties
# MCP Server STDIO 传输配置
spring.ai.mcp.server.transport=stdio
spring.main.web-application-type=none
```

说明：
- `spring.ai.mcp.server.transport=stdio` 启用 STDIO 传输协议
- `spring.main.web-application-type=none` 禁用 Web 容器，因为 STDIO 模式下不需要 HTTP 服务器

- [ ] **Step 2: 提交**

```bash
git add . && git commit -m "feat: 配置 STDIO 传输协议"
```

---

## Task 10: 验证构建

**文件：**
- 修改: `pom.xml`

- [ ] **Step 1: 添加测试用 H2 数据库依赖**

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 运行完整测试**

```bash
./mvnw clean test
```

预期：所有测试通过

- [ ] **Step 3: 验证编译**

```bash
./mvnw clean compile
```

预期：编译成功，无错误

- [ ] **Step 4: 提交**

```bash
git add . && git commit -m "test: 验证构建和测试"
```

---

## 执行方式

**推荐：Subagent-Driven** — 每个 Task 由独立的子 agent 执行，任务间有检查点，适合迭代式开发。

**备选：Inline Execution** — 在当前 session 内按批次执行，带检查点回顾。
