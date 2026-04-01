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
