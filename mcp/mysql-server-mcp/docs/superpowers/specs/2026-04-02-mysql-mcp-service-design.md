# MySQL MCP Service 实现设计

## 1. 概述

- **目标**：在 `MySQLMcpService` 类中实现三个 MCP Tool，供 AI 通过 MCP 协议执行 SQL
- **位置**：`src/main/java/com/gk/llm/mcp/mysqlservermcp/tools/MySQLMcpService.java`
- **依赖**：`JdbcUtils`（SQL 执行）、`SqlSafetyUtils`（安全检查）

## 2. 技术方案

使用 Spring AI MCP 的 `@McpToolShop` + `@McpTool` 注解方式，Spring 自动扫描注册 Tool。

## 3. MCP Tools

| Tool | 功能 | 参数 |
|------|------|------|
| `execute_query` | 执行 SELECT 查询（只读） | `sql: String`, `args: Object...` |
| `execute_update` | 执行 INSERT/UPDATE/DELETE | `sql: String`, `args: Object...` |
| `execute_ddl` | 执行 DDL（CREATE/ALTER 等） | `sql: String` |

## 4. MySQLMcpService 实现

```java
package com.gk.llm.mcp.mysqlservermcp.tools;

import com.gk.llm.mcp.mysqlservermcp.utils.JdbcUtils;
import org.springframework.ai.tool.annotation.ToolArg;
import org.springframework.ai.tool.annotation.ToolCall;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.shop.Tools;
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

## 5. 返回格式

由 `JdbcUtils` 统一返回 JSON 格式：

### 查询成功（SELECT）
```json
{
  "success": true,
  "data": [{"id": 1, "name": "zhangsan"}],
  "rowCount": 1
}
```

### 更新成功（INSERT/UPDATE/DELETE）
```json
{
  "success": true,
  "affectedRows": 3
}
```

### DDL 成功
```json
{
  "success": true,
  "message": "执行成功"
}
```

### 执行失败
```json
{
  "success": false,
  "error": "Table 'user' doesn't exist"
}
```

### 危险 SQL 拒绝
```json
{
  "success": false,
  "error": "危险SQL，已拒绝执行: DROP TABLE user"
}
```

## 6. 安全检查

内部由 `JdbcUtils` 调用 `SqlSafetyUtils.isSafe()` 实现：
- SELECT：任意
- UPDATE/DELETE：必须带 WHERE
- DROP/TRUNCATE/GRANT/REVOKE：拒绝
- CREATE/DESCRIBE/SHOW/ALTER：允许

## 7. 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `src/main/java/com/gk/llm/mcp/mysqlservermcp/tools/MySQLMcpService.java` |

## 8. 依赖关系

```
MySQLMcpService
    └── JdbcUtils (注入)
            ├── JdbcTemplate (Spring 注入)
            └── SqlSafetyUtils (静态调用)
```
