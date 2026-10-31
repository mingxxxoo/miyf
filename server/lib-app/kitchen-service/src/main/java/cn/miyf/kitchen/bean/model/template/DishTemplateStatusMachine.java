package cn.miyf.kitchen.bean.model.template;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;

import java.util.Map;
import java.util.Set;

/**
 * 菜品模板状态机：约束草稿 / 预创建 / 已发布 / 已归档之间的合法流转。
 * <p>
 * 正常链路：draft 或 pre_created → published → archived；
 * 归档后仅允许恢复为 draft，再重新发布，避免直接复用已归档内容。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
public final class DishTemplateStatusMachine {

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            DishTemplateStatus.DRAFT, Set.of(
                    DishTemplateStatus.PRE_CREATED, DishTemplateStatus.PUBLISHED, DishTemplateStatus.ARCHIVED),
            DishTemplateStatus.PRE_CREATED, Set.of(
                    DishTemplateStatus.DRAFT, DishTemplateStatus.PUBLISHED, DishTemplateStatus.ARCHIVED),
            DishTemplateStatus.PUBLISHED, Set.of(DishTemplateStatus.ARCHIVED),
            DishTemplateStatus.ARCHIVED, Set.of(DishTemplateStatus.DRAFT)
    );

    private DishTemplateStatusMachine() {
    }

    /**
     * 判断是否允许状态流转。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true 表示允许
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public static boolean canTransit(String from, String to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * 断言状态流转合法，否则抛出业务异常。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public static void assertTransit(String from, String to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS,
                    "菜品模板当前状态为 " + from + "，不能变更为 " + to);
        }
    }

    /**
     * 仅草稿与预创建允许直接编辑内容。
     *
     * @param status 当前状态
     * @return true 表示可编辑
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public static boolean editable(String status) {
        return DishTemplateStatus.DRAFT.equals(status) || DishTemplateStatus.PRE_CREATED.equals(status);
    }
}
