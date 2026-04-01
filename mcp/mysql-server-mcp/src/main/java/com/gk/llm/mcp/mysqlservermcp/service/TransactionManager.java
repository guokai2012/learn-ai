package com.gk.llm.mcp.mysqlservermcp.service;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 自动事务管理器
 * 用于包装多个数据库操作，确保原子性
 */
public class TransactionManager {

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 执行带事务的回调
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T execute(TransactionCallback<T> action) {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
        TransactionStatus status = txManager.getTransaction(txDefinition);

        try {
            T result = action.doInTransaction();
            txManager.commit(status);
            return result;
        } catch (Exception e) {
            txManager.rollback(status);
            throw new RuntimeException("事务执行失败，已回滚: " + e.getMessage(), e);
        }
    }

    /**
     * 事务回调接口
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction() throws SQLException;
    }
}
