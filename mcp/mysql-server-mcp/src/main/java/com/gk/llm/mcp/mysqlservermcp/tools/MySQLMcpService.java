package com.gk.llm.mcp.mysqlservermcp.tools;

import com.gk.llm.mcp.mysqlservermcp.utils.JdbcUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class MySQLMcpService {

    private final JdbcUtils jdbcUtils;

    public MySQLMcpService(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    @Tool(name = "execute_query", description = "执行 SELECT 查询，返回 JSON 格式结果")
    public String executeQuery(String sql, Object... args) {
        return jdbcUtils.executeQuery(sql, args);
    }

    @Tool(name = "execute_update", description = "执行 INSERT/UPDATE/DELETE，返回影响行数")
    public String executeUpdate(String sql, Object... args) {
        return jdbcUtils.executeUpdate(sql, args);
    }

    @Tool(name = "execute_ddl", description = "执行 DDL 语句（CREATE/ALTER/DROP 等）")
    public String executeDDL(String sql) {
        return jdbcUtils.executeDDL(sql);
    }
}