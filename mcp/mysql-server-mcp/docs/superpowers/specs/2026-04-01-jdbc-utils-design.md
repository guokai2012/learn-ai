# JdbcUtils 工具类设计

## 概述

创建一个通用的 JDBC SQL 执行工具类，提供单一方法 `execute(sql)` 自动判断 SQL 类型并执行。

## 位置

- 包名：`com.gk.llm.mcp.mysqlservermcp.utils`
- 类名：`JdbcUtils`

## 依赖

使用 Spring 的 `@Value` 注解从配置文件读取数据库连接参数：

- `@Value("spring.datasource.url")` - JDBC URL
- `@Value("spring.datasource.username")` - 数据库用户名
- `@Value("spring.datasource.password")` - 数据库密码

## 功能

### 核心方法

```java
public String execute(String sql)
```

### 自动路由逻辑

根据 SQL 首词自动判断类型并调用对应的 JDBC 方法：

| SQL 首词 | JDBC 方法 | 返回类型 |
|----------|-----------|----------|
| SELECT | Statement.executeQuery() | QUERY |
| INSERT | Statement.executeUpdate() | UPDATE |
| UPDATE | Statement.executeUpdate() | UPDATE |
| DELETE | Statement.executeUpdate() | UPDATE |
| 其他 | Statement.execute() | DDL |

### 返回 JSON 格式

**查询成功 (QUERY)**
```json
{
  "success": true,
  "type": "QUERY",
  "data": [{"id": 1, "name": "zhangsan"}],
  "rowCount": 1
}
```

**更新/删除成功 (UPDATE)**
```json
{
  "success": true,
  "type": "UPDATE",
  "affectedRows": 3
}
```

**DDL 成功 (DDL)**
```json
{
  "success": true,
  "type": "DDL",
  "message": "CREATE TABLE executed"
}
```

**执行失败 (ERROR)**
```json
{
  "success": false,
  "type": "ERROR",
  "error": "Table 'xxx' doesn't exist"
}
```

## 实现要点

1. 使用原生 JDBC（`java.sql.Connection`、`Statement`），不依赖 DataSource
2. 使用 `DriverManager.getConnection()` 获取连接
3. 连接使用后立即关闭（try-with-resources）
4. 查询结果通过 `ResultSetMetaData` 获取列名，`ResultSet.getObject()` 获取值
5. 所有结果和异常都包装为 JSON 字符串返回

## 文件结构

```
src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/
└── JdbcUtils.java
```
