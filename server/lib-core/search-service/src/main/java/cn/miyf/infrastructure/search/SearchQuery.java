package cn.miyf.infrastructure.search;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索请求模型。
 * 组合关键词 multi_match、term/terms 过滤、分页与排序；链式 setter 便于业务拼装。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public class SearchQuery {

    /** 逻辑或物理索引名。 */
    private String index;
    /** 关键词；为空则仅按过滤条件查询。 */
    private String keyword;
    /** multi_match 字段，可含权重写法如 name^3。 */
    private List<String> fields = new ArrayList<>();
    /** term/terms 过滤条件，保持插入顺序。 */
    private final Map<String, Object> filters = new LinkedHashMap<>();
    /** 分页起始偏移。 */
    private int from = 0;
    /** 页大小，至少为 1。 */
    private int size = 20;
    /** 排序列表。 */
    private final List<SearchSort> sorts = new ArrayList<>();

    /**
     * 获取索引名。
     *
     * @return 逻辑或物理索引名
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public String getIndex() {
        return index;
    }

    /**
     * 设置索引名。
     *
     * @param index 索引名
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery setIndex(String index) {
        this.index = index;
        return this;
    }

    /**
     * 获取关键词。
     *
     * @return 关键词
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置关键词；为空时查询退化为仅过滤或 match_all。
     *
     * @param keyword 关键词
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    /**
     * 获取 multi_match 目标字段。
     *
     * @return 字段列表（可含权重）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * 设置 multi_match 字段；传入 null 时重置为空列表。
     *
     * @param fields 字段列表
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery setFields(List<String> fields) {
        this.fields = fields == null ? new ArrayList<>() : new ArrayList<>(fields);
        return this;
    }

    /**
     * 获取过滤条件表。
     *
     * @return term 过滤（值可为单值或 Collection）
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Map<String, Object> getFilters() {
        return filters;
    }

    /**
     * 添加一条 term/terms 过滤；字段或值为空时忽略。
     *
     * @param field 字段名
     * @param value 过滤值
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery filter(String field, Object value) {
        if (field != null && !field.isBlank() && value != null) {
            filters.put(field, value);
        }
        return this;
    }

    /**
     * 获取分页偏移。
     *
     * @return 偏移
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public int getFrom() {
        return from;
    }

    /**
     * 设置分页偏移；负值会被纠正为 0。
     *
     * @param from 偏移
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery setFrom(int from) {
        this.from = Math.max(0, from);
        return this;
    }

    /**
     * 获取页大小。
     *
     * @return 页大小
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public int getSize() {
        return size;
    }

    /**
     * 设置页大小；小于 1 时纠正为 1，避免非法 size。
     *
     * @param size 页大小
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery setSize(int size) {
        this.size = Math.max(1, size);
        return this;
    }

    /**
     * 获取排序列表。
     *
     * @return 排序项
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<SearchSort> getSorts() {
        return sorts;
    }

    /**
     * 追加一条排序；null 忽略。
     *
     * @param sort 排序项
     * @return this
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public SearchQuery sort(SearchSort sort) {
        if (sort != null) {
            sorts.add(sort);
        }
        return this;
    }
}
