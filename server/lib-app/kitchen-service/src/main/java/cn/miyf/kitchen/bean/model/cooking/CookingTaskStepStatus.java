package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;

import java.util.Locale;

/**
 * 做菜任务步骤状态。
 * <p>
 * PENDING 未开始、RUNNING 进行中、PAUSED 已暂停、DONE 已完成；
 * 步骤计时与任务计时相互独立，任务暂停时会把进行中的步骤一并冻结。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
public enum CookingTaskStepStatus {

    /**
     * 未开始
     */
    PENDING,
    /**
     * 进行中
     */
    RUNNING,
    /**
     * 已暂停
     */
    PAUSED,
    /**
     * 已完成
     */
    DONE;

    /**
     * 解析状态文本，非法值抛出业务异常。
     *
     * @param value 状态文本
     * @return 步骤状态
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public static CookingTaskStepStatus from(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "步骤状态为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "未知步骤状态：" + value);
        }
    }

    /**
     * 是否已结束。
     *
     * @return true 表示已完成
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    public boolean finished() {
        return this == DONE;
    }
}
