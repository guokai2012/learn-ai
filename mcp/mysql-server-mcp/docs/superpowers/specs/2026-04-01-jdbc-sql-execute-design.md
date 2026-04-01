# JDBC SQL 执行工具类设计

## 1. 概述

- **功能**：提供通用的 SQL 执行工具，使用 JdbcTemplate 参数化查询防止 SQL 注入
- **位置**：`com.gk.llm.mcp.mysqlservermcp.utils.JdbcUtils`
- **辅助类**：`com.gk.llm.mcp.mysqlservermcp.utils.SqlSafetyUtils`
- **依赖**：Spring Boot 的 `@Value` 注解从配置文件读取数据库连接参数，JdbcTemplate 执行 SQL

## 2. 配置读取

工具类通过 Spring 的 `@Value` 注解从 `application.properties` 读取数据库连接配置：

| 字段 | @Value 注解 | 说明 |
|------|------------|------|
| URL | `spring.datasource.url` | JDBC 连接 URL |
| 用户名 | `spring.datasource.username` | 数据库用户名 |
| 密码 | `spring.datasource.password` | 数据库密码 |

## 3. JdbcUtils 类设计

```java
@Component
public class JdbcUtils {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUtils(@Value("${spring.datasource.url}") String url,
                     @Value("${spring.datasource.username}") String username,
                     @Value("${spring.datasource.password}") String password) {
        // 初始化 JdbcTemplate
        jdbcTemplate = new JdbcTemplate(createDataSource(url, username, password));
    }

    /**
     * 执行 SELECT 查询（参数化，防止注入）
     * @param sql 带占位符的 SQL
     * @param args 参数值
     * @return JSON 格式结果
     */
    public String executeQuery(String sql, Object... args)

    /**
     * 执行 INSERT/UPDATE/DELETE（参数化，防止注入）
     * @param sql 带占位符的 SQL
     * @param args 参数值
     * @return JSON 格式结果
     */
    public String executeUpdate(String sql, Object... args)

    /**
     * 执行 DDL（CREATE、ALTER 等）
     * @param sql DDL 语句
     * @return JSON 格式结果
     */
    public String executeDDL(String sql)
}
```

## 4. SqlSafetyUtils 安全检查规则

```java
public class SqlSafetyUtils {

    /**
     * 检查 SQL 是否安全可执行
     * @param sql 原始 SQL 语句
     * @return true=安全可执行，false=危险拒绝执行
     */
    public static boolean isSafe(String sql)
}
```

### 安全检查规则

| SQL 首词 | 允许条件 | 拒绝条件 |
|---------|---------|---------|
| SELECT | 任意（查询数据） | - |
| UPDATE | 必须包含 WHERE 子句（带条件更新） | 无 WHERE → 拒绝 |
| DELETE | 必须包含 WHERE 子句（带条件删除） | 无 WHERE → 拒绝 |
| CREATE TABLE/INDEX | 任意（建表、建索引） | - |
| DESCRIBE / SHOW | 任意（查看表结构） | - |
| ALTER TABLE | 任意（修改表结构） | - |
| DROP / TRUNCATE / DELETE 无条件 | - | 全部拒绝 |
| GRANT / REVOKE | - | 全部拒绝 |

### 拒绝执行返回 JSON
```json
{
  "success": false,
  "error": "危险SQL，已拒绝执行: TRUNCATE TABLE user"
}
```

## 5. 执行流程

```
executeQuery(sql, args)
  → SqlSafetyUtils.isSafe(sql) → false → 返回拒绝 JSON
  → true → JdbcTemplate.query() → 返回结果 JSON

executeUpdate(sql, args)
  → SqlSafetyUtils.isSafe(sql) → false → 返回拒绝 JSON
  → true → JdbcTemplate.update() → 返回影响行数 JSON

executeDDL(sql)
  → SqlSafetyUtils.isSafe(sql) → false → 返回拒绝 JSON
  → true → JdbcTemplate.execute() → 返回成功 JSON
```

## 6. 返回 JSON 格式

### 查询成功（SELECT）
```json
{
  "success": true,
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
  "affectedRows": 3
}
```

### DDL 成功（CREATE/ALTER/DROP 等）
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

## 7. 内部实现要点

1. **防止注入**：使用 JdbcTemplate 的参数化查询（`?` 占位符），参数通过 args 可变参数传入
2. **安全检查**：执行前调用 `SqlSafetyUtils.isSafe(sql)` 检查
3. **结果集转换**：查询结果通过 `ResultSetMetaData` 获取列名，使用 `getObject()` 获取值并转换为字符串
4. **异常处理**：捕获 `SQLException`，包装为 JSON 错误格式返回
5. **DataSource 创建**：使用 `DriverManagerDataSource` 或 `SimpleDriverDataSource` 创建 DataSource 供 JdbcTemplate 使用

## 8. 依赖关系

- Spring Boot 框架（提供 `@Value` 注解支持）
- Spring JDBC（`spring-boot-starter-jdbc` 提供 JdbcTemplate）
- Jackson（用于 JSON 序列化，项目中已引入）
