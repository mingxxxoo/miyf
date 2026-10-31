package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;

/**
 * 做菜任务状态。
 * <p>
 * 正常链路：PENDING → CLAIMED → PREPARING → COOKING ⇄ PAUSED → COMPLETED；
 * 未开始制作前可由提交人或领取厨师取消。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
public enum CookingTaskStatus {

    /**
     * 待领取
     */
    PENDING,
    /**
     * 已被领取
     */
    CLAIMED,
    /**
     * 备料中
     */
    PREPARING,
    /**
     * 制作中
     */
    COOKING,
    /**
     * 暂停中
     */
    PAUSED,
    /**
     * 已完成
     */
    COMPLETED,
    /**
     * 已取消
     */
    CANCELLED;

    /**
     * 从字符串解析状态，非法值抛出业务异常。
     *
     * @param value 状态名
     * @return 枚举值
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static CookingTaskStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务状态不能为空");
        }
        try {
            return CookingTaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务状态不合法：" + value);
        }
    }

    /**
     * 是否终态；终态不可再流转。
     *
     * @return true 表示已完成或已取消
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public boolean terminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
