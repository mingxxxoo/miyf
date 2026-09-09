package cn.miyf.kitchen.bean.model;

/**
 * 预约单状态枚举。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public enum OrderStatus {
    /**
     * 待确认
     */
    PENDING,
    /**
     * 已确认
     */
    CONFIRMED,
    /**
     * 准备中
     */
    PREPARING,
    /**
     * 已做好待取
     */
    READY,
    /**
     * 已完成
     */
    COMPLETED,
    /**
     * 已取消
     */
    CANCELLED;

    /**
     * 从字符串解析状态。
     *
     * @param value 状态名
     * @return 枚举值
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public static OrderStatus from(String value) {
        return OrderStatus.valueOf(value);
    }
}
