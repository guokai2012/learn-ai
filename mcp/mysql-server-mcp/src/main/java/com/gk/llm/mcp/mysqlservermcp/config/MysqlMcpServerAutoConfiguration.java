package com.gk.llm.mcp.mysqlservermcp;

import com.gk.llm.mcp.mysqlservermcp.config.DataSourceProperties;
import com.gk.llm.mcp.mysqlservermcp.security.SqlGuard;
import com.gk.llm.mcp.mysqlservermcp.service.TransactionManager;
import com.gk.llm.mcp.mysqlservermcp.tool.ExecuteDDlTool;
import com.gk.llm.mcp.mysqlservermcp.tool.ExecuteQueryTool;
import com.gk.llm.mcp.mysqlservermcp.tool.ExecuteUpdateTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MCP Server 自动配置
 */
@Configuration
@EnableAutoConfiguration
public class MysqlMcpServerAutoConfiguration {

    // 提供一个简单的 DataSource Bean，供 TransactionManager 使用
    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return new org.apache.commons.dbcp2.BasicDataSource() {{
            setUrl(properties.getUrl());
            setUsername(properties.getUsername());
            setPassword(properties.getPassword());
            setDriverClassName("com.mysql.cj.jdbc.Driver");
        }};
    }

    @Bean
    public SqlGuard sqlGuard() {
        return new SqlGuard();
    }

    @Bean
    public TransactionManager mysqlTransactionManager(DataSource dataSource) {
        return new TransactionManager(dataSource);
    }

    @Bean
    public ExecuteQueryTool executeQueryTool(DataSource dataSource, TransactionManager transactionManager) {
        return new ExecuteQueryTool(dataSource, transactionManager);
    }

    @Bean
    public ExecuteUpdateTool executeUpdateTool(DataSource dataSource,
                                                TransactionManager transactionManager) {
        return new ExecuteUpdateTool(dataSource, transactionManager);
    }

    @Bean
    public ExecuteDDlTool executeDDlTool(DataSource dataSource,
                                          TransactionManager transactionManager,
                                          SqlGuard sqlGuard) {
        return new ExecuteDDlTool(dataSource, transactionManager, sqlGuard);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ExecuteQueryTool executeQueryTool,
                                                     ExecuteUpdateTool executeUpdateTool,
                                                     ExecuteDDlTool executeDDlTool) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(executeQueryTool, executeUpdateTool, executeDDlTool)
            .build();
    }
}