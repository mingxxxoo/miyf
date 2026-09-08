package cn.miyf.common.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 审计时间 SQL 改写：手写 UPDATE/INSERT 自动补审计列。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class AuditSqlRewriter {

    private static final Pattern UPDATE_PREFIX =
            Pattern.compile("^\\s*UPDATE\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern INSERT_PREFIX =
            Pattern.compile("^\\s*INSERT\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LAST_MODIFY_ASSIGN =
            Pattern.compile("\\blast_modify_time\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_KEYWORD = Pattern.compile("\\bSET\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_VALUES = Pattern.compile(
            "(?is)^(\\s*INSERT\\s+INTO\\s+[\\w.\"`]+)(\\s*)\\(([^)]*)\\)(\\s*VALUES\\s*)(.*)$");
    private static final Pattern INSERT_SELECT = Pattern.compile(
            "(?is)^(\\s*INSERT\\s+INTO\\s+[\\w.\"`]+)(\\s*)\\(([^)]*)\\)(\\s*SELECT\\s+)(.+)$");
    private static final Pattern COLUMN_TOKEN = Pattern.compile("[\\w.\"`]+");

    private AuditSqlRewriter() {
    }

    /**
     * 按语句类型改写：UPDATE 补 last_modify_time；INSERT 补 create_time / last_modify_time。
     *
     * @param sql 原始 SQL
     * @return 改写后 SQL；无需改写时返回原串
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static String rewrite(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        if (UPDATE_PREFIX.matcher(sql).find()) {
            return rewriteUpdate(sql);
        }
        if (INSERT_PREFIX.matcher(sql).find()) {
            return rewriteInsert(sql);
        }
        return sql;
    }

    /**
     * 若为 UPDATE 且尚未赋值 {@code last_modify_time}，则在 SET 后注入 {@code last_modify_time = NOW()}。
     *
     * @param sql 原始 SQL
     * @return 改写后 SQL；无需改写时返回原串
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static String rewriteUpdate(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        if (!UPDATE_PREFIX.matcher(sql).find()) {
            return sql;
        }
        if (LAST_MODIFY_ASSIGN.matcher(sql).find()) {
            return sql;
        }
        Matcher setMatcher = SET_KEYWORD.matcher(sql);
        if (!setMatcher.find()) {
            return sql;
        }
        int insertAt = setMatcher.end();
        return sql.substring(0, insertAt) + " last_modify_time = NOW()," + sql.substring(insertAt);
    }

    /**
     * 若为带列清单的 INSERT，且缺少审计列，则补列并写入 {@code NOW()}。
     * 支持 {@code VALUES} 多行与 {@code INSERT ... SELECT}（顶层 FROM 前注入）。
     *
     * @param sql 原始 SQL
     * @return 改写后 SQL；无需改写或无法识别时返回原串
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static String rewriteInsert(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        if (!INSERT_PREFIX.matcher(sql).find()) {
            return sql;
        }

        Matcher valuesMatcher = INSERT_VALUES.matcher(sql);
        if (valuesMatcher.matches()) {
            List<String> missing = missingAuditColumns(valuesMatcher.group(3));
            if (missing.isEmpty()) {
                return sql;
            }
            return buildInsertValues(
                    valuesMatcher.group(1),
                    valuesMatcher.group(2),
                    valuesMatcher.group(3),
                    valuesMatcher.group(4),
                    valuesMatcher.group(5),
                    missing);
        }

        Matcher selectMatcher = INSERT_SELECT.matcher(sql);
        if (selectMatcher.matches()) {
            List<String> missing = missingAuditColumns(selectMatcher.group(3));
            if (missing.isEmpty()) {
                return sql;
            }
            String newSelectBody = injectBeforeTopLevelFrom(
                    selectMatcher.group(5),
                    ", " + String.join(", ", missing.stream().map(c -> "NOW()").toList()));
            if (newSelectBody.equals(selectMatcher.group(5))) {
                return sql;
            }
            String newColumns = appendColumns(
                    selectMatcher.group(3),
                    ", " + String.join(", ", missing));
            return selectMatcher.group(1)
                    + selectMatcher.group(2)
                    + "(" + newColumns + ")"
                    + selectMatcher.group(4)
                    + newSelectBody;
        }
        return sql;
    }

    private static String buildInsertValues(String intoPrefix,
                                            String spaceBeforeParen,
                                            String columns,
                                            String valuesKeyword,
                                            String valuesBody,
                                            List<String> missing) {
        String extraCols = ", " + String.join(", ", missing);
        String extraVals = ", " + String.join(", ", missing.stream().map(c -> "NOW()").toList());
        String newColumns = appendColumns(columns, extraCols);
        String newValuesBody = appendToValueTuples(valuesBody, extraVals);
        return intoPrefix + spaceBeforeParen + "(" + newColumns + ")" + valuesKeyword + newValuesBody;
    }

    private static List<String> missingAuditColumns(String columns) {
        List<String> missing = new ArrayList<>(2);
        if (!containsColumn(columns, "create_time")) {
            missing.add("create_time");
        }
        if (!containsColumn(columns, "last_modify_time")) {
            missing.add("last_modify_time");
        }
        return missing;
    }

    private static boolean containsColumn(String columns, String name) {
        Matcher matcher = COLUMN_TOKEN.matcher(columns);
        while (matcher.find()) {
            String token = matcher.group().replace("\"", "").replace("`", "").toLowerCase(Locale.ROOT);
            int dot = token.lastIndexOf('.');
            if (dot >= 0) {
                token = token.substring(dot + 1);
            }
            if (name.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static String appendColumns(String columns, String extraCols) {
        String trimmed = columns.trim();
        if (trimmed.isEmpty()) {
            return extraCols.substring(2);
        }
        if (trimmed.endsWith(",")) {
            return trimmed + " " + extraCols.substring(2);
        }
        return trimmed + extraCols;
    }

    private static String appendToValueTuples(String valuesBody, String extraVals) {
        StringBuilder out = new StringBuilder(valuesBody.length() + extraVals.length() * 2);
        int depth = 0;
        for (int i = 0; i < valuesBody.length(); i++) {
            char c = valuesBody.charAt(i);
            if (c == '(') {
                depth++;
                out.append(c);
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    out.append(extraVals);
                }
                out.append(c);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String injectBeforeTopLevelFrom(String selectBody, String extraSelect) {
        int depth = 0;
        for (int i = 0; i < selectBody.length(); i++) {
            char c = selectBody.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && regionEqualsIgnoreCase(selectBody, i, "from")) {
                boolean boundaryBefore = i == 0 || !isIdentChar(selectBody.charAt(i - 1));
                boolean boundaryAfter = i + 4 >= selectBody.length() || !isIdentChar(selectBody.charAt(i + 4));
                if (boundaryBefore && boundaryAfter) {
                    String head = selectBody.substring(0, i).stripTrailing();
                    return head + extraSelect + " " + selectBody.substring(i);
                }
            }
        }
        return selectBody;
    }

    private static boolean regionEqualsIgnoreCase(String text, int offset, String word) {
        if (offset + word.length() > text.length()) {
            return false;
        }
        return text.regionMatches(true, offset, word, 0, word.length());
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
