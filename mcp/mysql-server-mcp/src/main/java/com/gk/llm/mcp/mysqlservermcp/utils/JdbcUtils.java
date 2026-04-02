package com.gk.llm.mcp.mysqlservermcp.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSetMetaData;
import java.util.List;

/**
 * JDBC SQL 执行工具类
 * 使用 JdbcTemplate 参数化查询防止 SQL 注入
 */
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
            List<ObjectNode> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
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

        } catch (Exception e) {
            return errorJson(e);
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

        } catch (Exception e) {
            return errorJson(e);
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

        } catch (Exception e) {
            return errorJson(e);
        }
    }

    /**
     * 生成危险 SQL 拒绝执行的 JSON 响应
     * @param sql 被拒绝的 SQL 语句
     * @return JSON 格式错误响应
     */
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

    /**
     * 生成错误 JSON 响应
     * @param error 错误信息
     * @return JSON 格式错误响应
     */
    private String errorJson(Exception e) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", false);
            // 获取根本原因
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String rootError = root.getMessage();
            String simpleError = rootError != null ? rootError : e.getMessage();
            simpleError = simpleError.length() > 200 ? simpleError.substring(0, 200) + "..." : simpleError;
            result.put("error", simpleError);
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "{\"success\":false,\"error\":\"未知错误\"}";
        }
    }
}
