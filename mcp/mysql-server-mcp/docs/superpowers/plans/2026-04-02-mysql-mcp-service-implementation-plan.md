# MySQL MCP Service 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 MySQLMcpService 类中实现三个 MCP Tool（execute_query/execute_update/execute_ddl），供 AI 通过 MCP 协议执行 SQL

**Architecture:** 使用 Spring AI MCP 的 `@Tool` 注解方式自动注册 Tool，依赖 JdbcUtils 执行 SQL 和安全检查

**Tech Stack:** Spring AI MCP Server 2.0.0-M4, Spring JDBC (JdbcTemplate), Jackson

---

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `src/main/java/com/gk/llm/mcp/mysqlservermcp/tools/MySQLMcpService.java` |

---

## Task 1: MySQLMcpService 实现

### 验收标准

- MySQLMcpService 是 Spring `@Component`，自动被 MCP Server 扫描注册
- 三个 Tool 方法：execute_query、execute_update、execute_ddl
- 每个 Tool 方法参数包含 name 和 description
- Tool 方法委托 JdbcUtils 执行实际 SQL

### Steps

- [ ] **Step 1: 编写 MySQLMcpService 实现**

文件：`src/main/java/com/gk/llm/mcp/mysqlservermcp/tools/MySQLMcpService.java`

```java
package com.gk.llm.mcp.mysqlservermcp.tools;

import com.gk.llm.mcp.mysqlservermcp.utils.JdbcUtils;
import org.springframework.ai.tool.annotation.ToolArg;
import org.springframework.ai.tool.annotation.ToolCall;
import org.springframework.stereotype.Component;

@Component
public class MySQLMcpService {

    private final JdbcUtils jdbcUtils;

    public MySQLMcpService(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    @ToolCall(name = "execute_query", description = "执行 SELECT 查询，返回 JSON 格式结果")
    public String executeQuery(
            @ToolArg(name = "sql", description = "SQL 查询语句，支持占位符参数") String sql,
            @ToolArg(name = "args", description = "查询参数（可选）") Object... args) {
        return jdbcUtils.executeQuery(sql, args);
    }

    @ToolCall(name = "execute_update", description = "执行 INSERT/UPDATE/DELETE，返回影响行数")
    public String executeUpdate(
            @ToolArg(name = "sql", description = "SQL 语句，支持占位符参数") String sql,
            @ToolArg(name = "args", description = "参数（可选）") Object... args) {
        return jdbcUtils.executeUpdate(sql, args);
    }

    @ToolCall(name = "execute_ddl", description = "执行 DDL 语句（CREATE/ALTER/DROP 等）")
    public String executeDDL(
            @ToolArg(name = "sql", description = "DDL 语句") String sql) {
        return jdbcUtils.executeDDL(sql);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd mcp/mysql-server-mcp && mvn compile -q`
Expected: 编译成功，无错误

如果编译失败，检查 Spring AI 注解类名是否正确。常见的替代注解：
- `@Tool` 替代 `@ToolCall`
- `@ToolInput` 替代 `@ToolArg`

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/gk/llm/mcp/mysqlservermcp/tools/MySQLMcpService.java
git commit -m "$(cat <<'EOF'
feat: 实现 MySQLMcpService 三个 MCP Tool

- execute_query: 执行 SELECT 查询
- execute_update: 执行 INSERT/UPDATE/DELETE
- execute_ddl: 执行 DDL 语句
- 使用 @ToolCall 注解自动注册

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: 验证 MCP Server 自动注册（可选）

### 背景

如果 Spring AI MCP Server 配置了自动扫描 `@Component` 类，MySQLMcpService 会被自动注册为 Tool。如果未自动扫描，可能需要手动配置。

### Steps

- [ ] **Step 1: 检查是否有 MCP Server 配置类**

Run: `grep -r "McpServer" --include="*.java" mcp/mysql-server-mcp/src/main/java/`
Expected: 查找是否有 McpServerAutoConfiguration 或类似配置类

- [ ] **Step 2: 如需要，创建 MCP Server 配置**

如果项目没有 MCP Server 自动配置，创建 `McpServerAutoConfiguration.java`：

```java
package com.gk.llm.mcp.mysqlservermcp.config;

import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MethodToolCallbackProvider methodToolCallbackProvider() {
        return new MethodToolCallbackProvider();
    }
}
```

- [ ] **Step 3: 提交配置变更（如有）**

```bash
git add src/main/java/com/gk/llm/mcp/mysqlservermcp/config/McpServerAutoConfiguration.java
git commit -m "$(cat <<'EOF'
feat: 添加 MCP Server 自动配置

- MethodToolCallbackProvider 自动扫描 @Component 中的 @Tool 方法

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## 执行方式

**Plan complete and saved to `docs/superpowers/plans/2026-04-02-mysql-mcp-service-implementation-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
