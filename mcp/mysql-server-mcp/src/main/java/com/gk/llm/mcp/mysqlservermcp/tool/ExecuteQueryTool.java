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

        } catch (RuntimeException e) {
            return "事务错误：" + e.getMessage();
        } catch (Exception e) {
            return "序列化错误：" + e.getMessage();
        }
    }
}