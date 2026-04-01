package com.gk.llm.mcp.mysqlservermcp.security;

import java.util.regex.Pattern;

/**
 * SQL 危险语句拦截器
 * 禁止执行高危 DDL 操作
 */
public class SqlGuard {

    // 拦截：DROP DATABASE 不带 IF EXISTS（IF EXISTS 是安全的）
    private static final Pattern DROP_DATABASE_PATTERN =
        Pattern.compile("^\\s*DROP\\s+DATABASE\\s+(?!IF\\s+EXISTS)\\w+\\s*$", Pattern.CASE_INSENSITIVE);

    // 拦截：TRUNCATE 不带表名（TRUNCATE 或 TRUNCATE TABLE）
    private static final Pattern TRUNCATE_NO_TABLE_PATTERN =
        Pattern.compile("^\\s*TRUNCATE\\s*(TABLE\\s*)?$", Pattern.CASE_INSENSITIVE);

    // 拦截：DROP TABLE * 或 DROP TABLE 无具体表名
    private static final Pattern DROP_TABLE_NO_NAME_PATTERN =
        Pattern.compile("^\\s*DROP\\s+TABLE\\s+(\\*|;)\\s*$", Pattern.CASE_INSENSITIVE);

    /**
     * 检查 SQL 是否为危险语句
     * @param sql SQL 语句
     * @return true 如果语句危险应被拦截
     */
    public boolean isDangerous(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmed = sql.trim();
        return DROP_DATABASE_PATTERN.matcher(trimmed).matches()
            || TRUNCATE_NO_TABLE_PATTERN.matcher(trimmed).matches()
            || DROP_TABLE_NO_NAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 获取危险语句描述
     * @param sql SQL 语句
     * @return 危险描述，如果安全则返回 null
     */
    public String getDangerDescription(String sql) {
        if (isDangerous(sql)) {
            if (DROP_DATABASE_PATTERN.matcher(sql.trim()).matches()) {
                return "危险操作：禁止删除整个数据库";
            }
            if (TRUNCATE_NO_TABLE_PATTERN.matcher(sql.trim()).matches()) {
                return "危险操作：TRUNCATE 必须指定表名";
            }
            if (DROP_TABLE_NO_NAME_PATTERN.matcher(sql.trim()).matches()) {
                return "危险操作：DROP TABLE 必须指定表名";
            }
            return "危险操作：未知类型";
        }
        return null;
    }
}