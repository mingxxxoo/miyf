package cn.miyf.infrastructure.search;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SQL 排序列 / 别名 → ES 文档字段映射。
 * 兼容 {@code d.sort_order}、{@code sort_order} 等写法。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public final class SearchFieldMapping {

    private final Map<String, String> aliases = new LinkedHashMap<>();

    private SearchFieldMapping() {
    }

    /**
     * 创建空映射（仍会剥离表别名前缀）。
     *
     * @return 映射
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static SearchFieldMapping create() {
        return new SearchFieldMapping();
    }

    /**
     * 注册别名：SQL 列名（可含表别名）→ ES 字段。
     *
     * @param sqlColumn SQL 列，如 d.is_recommend / sort_order
     * @param esField   ES 字段，如 recommend / sortOrder
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchFieldMapping map(String sqlColumn, String esField) {
        if (sqlColumn == null || sqlColumn.isBlank() || esField == null || esField.isBlank()) {
            return this;
        }
        String key = normalizeKey(sqlColumn);
        aliases.put(key, esField.trim());
        // 无表一份无表别名的键，便于 d.col / col 互通
        String bare = stripAlias(key);
        if (!bare.equals(key)) {
            aliases.putIfAbsent(bare, esField.trim());
        }
        return this;
    }

    /**
     * 解析 ES 字段名；未注册时返回去表别名后的原列名。
     *
     * @param sqlColumn SQL 列
     * @return ES 字段
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public String resolve(String sqlColumn) {
        if (sqlColumn == null || sqlColumn.isBlank()) {
            return sqlColumn;
        }
        String key = normalizeKey(sqlColumn);
        String mapped = aliases.get(key);
        if (mapped != null) {
            return mapped;
        }
        String bare = stripAlias(key);
        mapped = aliases.get(bare);
        if (mapped != null) {
            return mapped;
        }
        return bare;
    }

    private static String normalizeKey(String column) {
        return column.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripAlias(String column) {
        int dot = column.lastIndexOf('.');
        return dot >= 0 ? column.substring(dot + 1) : column;
    }
}
