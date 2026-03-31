# MySQL MCP Server 设计文档

## 概述

基于 Spring AI 的 MySQL MCP Server，允许 AI 通过 MCP 协议执行 DQL、DML、DDL 语句，并提供自动事务管理和危险语句拦截功能。

## 功能范围

### 支持的 SQL 类型

| 类型 | 语句 |
|------|------|
| DQL | SELECT |
| DML | INSERT、UPDATE、DELETE |
| DDL | CREATE、ALTER、DROP、TRUNCATE 等 |

### 事务管理

- AI 执行的多个操作自动在同一事务内
- 全部成功自动 COMMIT
- 任何操作失败自动 ROLLBACK

### 数据库连接

- 动态连接：按需创建/释放连接
- 适合低频或间歇性查询场景

### 安全防护

- 危险语句拦截：
  - `DROP DATABASE`（不带表名）
  - `TRUNCATE`（不带表名）
  - `DROP TABLE *`（全局删除）
- 保留必要的 DDL 能力

### 传输协议

- STDIO：通过标准输入/输出与 MCP 客户端通信

## 项目结构

```
src/main/java/com/gk/llm/mcp/mysqlservermcp/
├── MysqlServerMcpApplication.java           # Spring Boot 入口
├── MysqlMcpServerAutoConfiguration.java     # MCP Server 自动配置
├── tool/                                    # MCP Tool 实现
│   ├── ExecuteQueryTool.java               # DQL 执行器
│   ├── ExecuteUpdateTool.java              # DML 执行器
│   └── ExecuteDDlTool.java                 # DDL 执行器（带危险语句检查）
├── service/
│   └── TransactionManager.java             # 自动事务管理
├── security/
│   └── SqlGuard.java                       # 危险语句拦截器
└── config/
    └── DataSourceConfig.java               # 动态数据源配置
```

## MCP Tools

### execute_query

- 功能：执行 SELECT 查询
- 输入：SQL 字符串
- 输出：查询结果（JSON 格式）
- 事务：只读事务

### execute_update

- 功能：执行 INSERT、UPDATE、DELETE
- 输入：SQL 字符串
- 输出：影响的行数
- 事务：自动事务管理

### execute_ddl

- 功能：执行 CREATE、ALTER、DROP、TRUNCATE 等
- 输入：SQL 字符串
- 输出：执行结果描述
- 安全：危险语句拦截
- 事务：自动事务管理

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| spring.datasource.url | JDBC URL | jdbc:mysql://localhost:3306/test |
| spring.datasource.username | 数据库用户名 | root |
| spring.datasource.password | 数据库密码 | - |

## 技术栈

- Spring Boot 4.0.5
- Spring AI 2.0.0-M4
- MySQL Connector/J
- Lombok
