package com.gk.llm.mcp.mysqlservermcp.tool;

import com.gk.llm.mcp.mysqlservermcp.security.SqlGuard;
import com.gk.llm.mcp.mysqlservermcp.service.TransactionManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DDL 执行器 - 处理 CREATE、ALTER、DROP、TRUNCATE 等
 * 包含危险语句拦截
 */
@Component
public class ExecuteDDlTool {

    private final DataSource dataSource;
    private final TransactionManager transactionManager;
    private final SqlGuard sqlGuard;

    public ExecuteDDlTool(DataSource dataSource,
                         TransactionManager transactionManager,
                         SqlGuard sqlGuard) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.sqlGuard = sqlGuard;
    }

    @Tool(description = "执行 CREATE、ALTER、DROP、TRUNCATE 等 DDL 语句")
    public String executeDDl(@ToolParam(description = "SQL DDL 语句") String sql) {
        // 危险语句检查
        String dangerDesc = sqlGuard.getDangerDescription(sql);
        if (dangerDesc != null) {
            return "拦截：" + dangerDesc + "，SQL：" + sql;
        }

        try {
            transactionManager.execute(() -> {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                return null;
            });

            return "成功执行 DDL：" + sql;

        } catch (SQLException e) {
            return "数据库错误：" + e.getMessage();
        } catch (RuntimeException e) {
            return "事务错误：" + e.getMessage();
        }
    }
}
