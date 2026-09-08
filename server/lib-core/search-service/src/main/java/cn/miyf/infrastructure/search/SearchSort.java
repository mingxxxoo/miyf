package cn.miyf.infrastructure.search;

/**
 * 搜索排序项。
 * field 为 {@code _score} 时表示按相关度排序，否则按指定字段排序。
 *
 * @param field 字段名或 {@code _score}
 * @param asc   是否升序
 * @author XieMingJie
 * @since 2026-09-08
 */
public record SearchSort(String field, boolean asc) {

    /**
     * 按相关度降序。
     *
     * @return 排序项
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static SearchSort scoreDesc() {
        return new SearchSort("_score", false);
    }

    /**
     * 按字段升序。
     *
     * @param field 字段名
     * @return 排序项
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static SearchSort asc(String field) {
        return new SearchSort(field, true);
    }

    /**
     * 按字段降序。
     *
     * @param field 字段名
     * @return 排序项
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static SearchSort desc(String field) {
        return new SearchSort(field, false);
    }
}
