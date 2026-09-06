package cn.miyf.common.query;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通用分页查询条件基类（QO）。
 * <p>
 * 适配当前架构：MyBatis-Plus {@link Page} + OpenAPI {@link Schema} + Lombok。
 * 不依赖 Vgo 专有分页实现。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "通用分页查询条件")
public abstract class AbstractCondition implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(AbstractCondition.class);

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String ORDER_FIELD_ERROR_MSG = "排序字段错误";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Schema(description = "当前页，从 1 开始")
    private int page = 1;

    @Schema(description = "每页数量")
    private int rows = 10;

    @Schema(description = "是否查询总数", hidden = true)
    private boolean queryTotal = true;

    @Schema(description = "是否查询明细")
    private boolean queryRecord = true;

    @Getter(lombok.AccessLevel.NONE)
    @Schema(description = "关键字")
    private String keyword;

    @Getter(lombok.AccessLevel.NONE)
    @Schema(description = "排序，如 created_at desc 或 name asc,created_at desc")
    private String sort;

    @Getter(lombok.AccessLevel.NONE)
    @Schema(description = "排序列集合", hidden = true)
    private List<SortColumn> sortList;

    @Schema(description = "查询统计 JSON，格式如：[{\"type\":\"sum\",\"field\":\"rating\"}]")
    private String aggregateJson;

    @Schema(description = "游标分页上一页最后一条主键", hidden = true)
    private Object lastKey;

    @Schema(description = "是否启用游标分页", hidden = true)
    private boolean cursorPaging = false;

    @Schema(description = "自定义查询 JSON")
    private String customQueryJson;

    /**
     * 构造 MyBatis-Plus 分页参数（不含排序）。
     *
     * @param <T> 记录类型
     * @return 分页对象
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public <T> Page<T> pageParam() {
        // queryTotal=false 时关闭 count，降低无谓扫描
        Page<T> pageParam = new Page<>(normalizePage(), normalizeRows(), isQueryTotal());
        if (!isQueryRecord()) {
            // 仅统计场景：不拉明细
            pageParam.setSize(0);
        }
        return pageParam;
    }

    /**
     * 构造分页参数，并应用请求排序或默认排序。
     *
     * @param defaultSorts 默认排序
     * @param <T>          记录类型
     * @return 分页对象
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public <T> Page<T> pageParamIncludeSort(OrderItem... defaultSorts) {
        Page<T> pageParam = pageParam();
        if (this.sort != null && this.sort.length() > 1) {
            addPageSort(pageParam);
        } else if (defaultSorts != null) {
            addDefaultSort(defaultSorts, pageParam);
        }
        return pageParam;
    }

    /**
     * 应用请求排序，并追加必须排序列（去重）。
     *
     * @param defaultSorts 默认排序
     * @param mustSort     必须追加的排序
     * @param <T>          记录类型
     * @return 分页对象
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public <T> Page<T> getIncludeSortPage(OrderItem defaultSorts, OrderItem mustSort) {
        Page<T> pageParam = pageParamIncludeSort(defaultSorts);
        if (mustSort != null) {
            pageParam.addOrder(mustSort);
        }
        // 同列只保留第一次出现，避免重复 order by
        Set<String> existsOrder = new HashSet<>();
        pageParam.orders().removeIf(orderItem -> {
            if (existsOrder.contains(orderItem.getColumn())) {
                return true;
            }
            existsOrder.add(orderItem.getColumn());
            return false;
        });
        return pageParam;
    }

    /**
     * 解析聚合统计参数。
     *
     * @return 聚合列表，解析失败抛业务异常
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public List<ConditionAggregate> statisticParamList() {
        if (this.aggregateJson == null || this.aggregateJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(this.aggregateJson, new TypeReference<List<ConditionAggregate>>() {
            });
        } catch (Exception e) {
            log.error("聚合统计参数错误", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "统计参数错误");
        }
    }

    /**
     * 关键字（自动 trim）。
     *
     * @return 关键字
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public String getKeyword() {
        if (this.keyword != null) {
            this.keyword = this.keyword.trim();
        }
        return this.keyword;
    }

    /**
     * SQL LIKE 形态关键字：%keyword%。
     *
     * @return like 串，空则 null
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    @Schema(hidden = true, description = "关键字 LIKE")
    public String getKeywordLike() {
        String kw = getKeyword();
        if (kw != null && !kw.isEmpty()) {
            return "%" + kw + "%";
        }
        return null;
    }

    /**
     * 大写 LIKE 关键字。
     *
     * @return like 串，空则 null
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    @Schema(hidden = true, description = "关键字大写 LIKE")
    public String getKeywordUpperLike() {
        String kw = getKeyword();
        if (kw != null && !kw.isEmpty()) {
            return "%" + kw.toUpperCase() + "%";
        }
        return null;
    }

    /**
     * 校验并返回排序原文。
     *
     * @return sort
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public String getSort() {
        if (this.sort != null && this.sort.length() > 1) {
            validateSortText(this.sort);
        }
        return this.sort;
    }

    /**
     * 解析排序列为结构化列表。
     *
     * @return 排序列
     * @history 1.00 2026-09-04 16:54 XieMingJie Created.
     */
    public List<SortColumn> getSortList() {
        if (this.sort == null || this.sort.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] sortArrays = this.sort.split(",");
        if (this.sortList != null && this.sortList.size() == sortArrays.length) {
            return this.sortList;
        }
        List<SortColumn> sortColumnList = new ArrayList<>();
        for (String sortSub : sortArrays) {
            String[] sarray = sortSub.trim().replace("  ", " ").split(" ");
            if (sarray.length < 2) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ORDER_FIELD_ERROR_MSG);
            }
            String column = sarray[0].trim();
            String orderType = sarray[1].trim();
            assertSafeColumn(column);
            sortColumnList.add(new SortColumn(column, SortType.valueOf(orderType.toUpperCase())));
        }
        this.sortList = sortColumnList;
        return this.sortList;
    }

    private <T> void addPageSort(Page<T> pageParam) {
        String[] sortArrays = this.sort.split(",");
        for (String sortSub : sortArrays) {
            String sortUpper = sortSub.trim().toUpperCase().replace("\t", " ");
            String[] sarray = sortUpper.split(" ");
            if (sarray.length < 2) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ORDER_FIELD_ERROR_MSG);
            }
            String column = sarray[0].trim();
            // 列名校验用原始小写/混合形式，避免把列名强制改成大写导致 SQL 失败
            String rawColumn = sortSub.trim().split("\\s+")[0].trim();
            assertSafeColumn(rawColumn);
            String orderType = sarray[1].trim();
            if (orderType.contains(SortType.ASC.name())) {
                pageParam.addOrder(OrderItem.asc(rawColumn));
            } else if (orderType.contains(SortType.DESC.name())) {
                pageParam.addOrder(OrderItem.desc(rawColumn));
            } else {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ORDER_FIELD_ERROR_MSG);
            }
        }
    }

    private static <T> void addDefaultSort(OrderItem[] defaultSorts, Page<T> pageParam) {
        for (OrderItem defaultSort : defaultSorts) {
            if (defaultSort != null) {
                pageParam.addOrder(defaultSort);
            }
        }
    }

    private void validateSortText(String sortText) {
        String[] sortArrays = sortText.split(",");
        for (String sortSub : sortArrays) {
            String sortUpper = sortSub.trim().toUpperCase().replace("\t", " ");
            String[] sarray = sortUpper.split(" ");
            if (sarray.length < 2) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ORDER_FIELD_ERROR_MSG);
            }
            String column = sortSub.trim().split("\\s+")[0].trim();
            String orderType = sarray[1].trim();
            assertSafeColumn(column);
            if (!SortType.ASC.name().equals(orderType) && !SortType.DESC.name().equals(orderType)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ORDER_FIELD_ERROR_MSG);
            }
        }
    }

    private void assertSafeColumn(String column) {
        // 防 SQL 注入：排序列仅允许标识符字符
        if (!column.matches("[a-zA-Z0-9_\\.]+")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ORDER_FIELD_ERROR_MSG);
        }
    }

    private int normalizePage() {
        return this.page < 1 ? 1 : this.page;
    }

    private int normalizeRows() {
        if (this.rows < 1) {
            return 10;
        }
        // 与 BaseApplicationService 上限对齐，防止超大分页
        return Math.min(this.rows, 100);
    }
}
