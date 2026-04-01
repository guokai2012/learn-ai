package com.gk.llm.mcp.mysqlservermcp.utils;

import java.util.regex.Pattern;

/**
 * SQL 安全检查工具类
 * 检查 SQL 是否在允许的安全范围内执行
 */
public class SqlSafetyUtils {

    // 危险 DDL：DROP
    private static final Pattern DROP_PATTERN = Pattern.compile(
        "^\\s*DROP\\s+", Pattern.CASE_INSENSITIVE);

    // 危险 DDL：TRUNCATE
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile(
        "^\\s*TRUNCATE\\s+", Pattern.CASE_INSENSITIVE);

    // 危险 DDL：GRANT/REVOKE
    private static final Pattern GRANT_REVOKE_PATTERN = Pattern.compile(
        "^\\s*(GRANT|REVOKE)\\s+", Pattern.CASE_INSENSITIVE);

    /**
     * 检查 SQL 是否安全可执行
     * @param sql SQL 语句
     * @return true=安全可执行，false=危险拒绝执行
     */
    public static boolean isSafe(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return true;
        }
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();

        // 允许：SELECT
        if (upper.startsWith("SELECT")) {
            return true;
        }

        // 允许：带 WHERE 的 UPDATE
        if (upper.startsWith("UPDATE") && !upper.contains("WHERE")) {
            return false;
        }

        // 允许：带 WHERE 的 DELETE
        if (upper.startsWith("DELETE") && !upper.contains("WHERE")) {
            return false;
        }

        // 允许：CREATE TABLE/INDEX
        if (upper.startsWith("CREATE")) {
            return true;
        }

        // 允许：DESCRIBE/SHOW
        if (upper.startsWith("DESCRIBE") || upper.startsWith("SHOW")) {
            return true;
        }

        // 允许：ALTER TABLE
        if (upper.startsWith("ALTER")) {
            return true;
        }

        // 拒绝：DROP
        if (DROP_PATTERN.matcher(trimmed).find()) {
            return false;
        }

        // 拒绝：TRUNCATE
        if (TRUNCATE_PATTERN.matcher(trimmed).find()) {
            return false;
        }

        // 拒绝：GRANT/REVOKE
        if (GRANT_REVOKE_PATTERN.matcher(trimmed).find()) {
            return false;
        }

        // 其他未分类的默认安全（INSERT 等）
        return true;
    }
}