# JDBC SQL 执行工具类设计

## 1. 概述

- **功能**：提供通用的 SQL 执行工具，通过原生 JDBC（DriverManager）直接执行 SQL 语句
- **位置**：`com.gk.llm.mcp.mysqlservermcp.utils.JdbcUtils`
- **依赖**：Spring Boot 的 `@Value` 注解从配置文件读取数据库连接参数

## 2. 配置读取

工具类通过 Spring 的 `@Value` 注解从 `application.properties` 读取数据库连接配置：

| 字段 | @Value 注解 | 说明 |
|------|------------|------|
| URL | `spring.datasource.url` | JDBC 连接 URL |
| 用户名 | `spring.datasource.username` | 数据库用户名 |
| 密码 | `spring.datasource.password` | 数据库密码 |

## 3. 方法签名

```java
@Component
public class JdbcUtils {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * 执行 SQL 语句，自动根据 SQL 类型路由
     * @param sql 完整的 SQL 语句
     * @return JSON 格式的执行结果
     */
    public String execute(String sql)
}
```

## 4. SQL 类型判断逻辑

根据 SQL 首词自动判断类型并路由：

| SQL 首词（忽略前后空格） | JDBC 方法 | 返回 type |
|------------------------|----------|-----------|
| SELECT | Statement.executeQuery() | QUERY |
| INSERT | Statement.executeUpdate() | UPDATE |
| UPDATE | Statement.executeUpdate() | UPDATE |
| DELETE | Statement.executeUpdate() | UPDATE |
| 其他（CREATE, ALTER, DROP 等） | Statement.execute() | DDL |

## 5. 返回 JSON 格式

### 查询成功（SELECT）
```json
{
  "success": true,
  "type": "QUERY",
  "data": [
    {"id": 1, "name": "zhangsan"},
    {"id": 2, "name": "lisi"}
  ],
  "rowCount": 2
}
```

### 更新成功（INSERT/UPDATE/DELETE）
```json
{
  "success": true,
  "type": "UPDATE",
  "affectedRows": 3
}
```

### DDL 成功（CREATE/ALTER/DROP 等）
```json
{
  "success": true,
  "type": "DDL",
  "message": "执行成功"
}
```

### 执行失败
```json
{
  "success": false,
  "type": "ERROR",
  "error": "Table 'user' doesn't exist"
}
```

## 6. 内部实现要点

1. **连接管理**：使用 `DriverManager.getConnection()` 获取原生连接
2. **资源释放**：使用 try-with-resources 确保 Connection、Statement、ResultSet 正确关闭
3. **结果集转换**：查询结果通过 `ResultSetMetaData` 获取列名，使用 `getObject()` 获取值并转换为字符串
4. **异常处理**：捕获 `SQLException`，包装为 JSON 错误格式返回

## 7. 依赖关系

- Spring Boot 框架（提供 `@Value` 注解支持）
- JDK 原生 JDBC（`java.sql.*`）
- Jackson（用于 JSON 序列化，项目中已引入）
