# JdbcUtils 优化设计

## 1. 概述

- **目标**：优化 JdbcUtils 依赖注入方式，清理无用依赖
- **位置**：`src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtils.java`
- **删除**：`src/test/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtilsTest.java`

## 2. JdbcUtils 改动

### 2.1 类注解
- 添加 `@Component` 注解，支持 Spring 自动注入

### 2.2 构造函数
- **移除**：手动创建 DataSource 的构造函数（供测试使用的 package-private 构造）
- **新增**：直接注入 `JdbcTemplate`

```java
@Component
public class JdbcUtils {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Spring 注入的构造函数
     * @param jdbcTemplate Spring 管理的 JdbcTemplate
     */
    public JdbcUtils(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    // ... 现有方法保持不变
}
```

## 3. 清理的依赖

| 依赖 | 原因 |
|------|------|
| `spring-boot-starter-test` | 不需要单元测试 |
| `commons-dbcp2` | 未使用 |
| `lombok` | 未使用（代码中无 @Data/@Getter 等注解） |
| `h2` | 测试依赖随测试代码一起删除 |

## 4. 文件变更

### 删除
- `src/test/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtilsTest.java`

### 修改
- `src/main/java/com/gk/llm/mcp/mysqlservermcp/utils/JdbcUtils.java`
- `pom.xml`
