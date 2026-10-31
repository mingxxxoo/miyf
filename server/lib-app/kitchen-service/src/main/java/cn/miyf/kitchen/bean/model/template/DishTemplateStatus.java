package cn.miyf.kitchen.bean.model.template;

import java.util.Locale;
import java.util.Set;

/**
 * 菜品模板（候选菜）状态常量及其合法取值。
 * <p>
 * 与设计文档保持一致：draft 草稿、pre_created 预创建、published 已发布、archived 已归档。
 * 模板状态独立于 {@code dish.status}（菜单上下架）与审核状态，避免影响现有点菜链路。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
public final class DishTemplateStatus {

    public static final String DRAFT = "draft";
    public static final String PRE_CREATED = "pre_created";
    public static final String PUBLISHED = "published";
    public static final String ARCHIVED = "archived";

    private static final Set<String> ALL = Set.of(DRAFT, PRE_CREATED, PUBLISHED, ARCHIVED);

    private DishTemplateStatus() {
    }

    /**
     * 是否为合法的模板状态（忽略大小写与首尾空白）。
     *
     * @param status 状态
     * @return true 表示合法
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public static boolean isValid(String status) {
        return status != null && ALL.contains(status.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 规范化为小写状态值。
     *
     * @param status 状态
     * @return 规范化状态；非法值原样去空白返回
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public static String normalize(String status) {
        return status == null ? null : status.trim().toLowerCase(Locale.ROOT);
    }
}
