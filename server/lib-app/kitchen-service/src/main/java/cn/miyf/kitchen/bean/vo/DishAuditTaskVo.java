package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 菜品审核待办展示。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Accessors(chain = true)
@Schema(name = "DishAuditTaskVo")
public class DishAuditTaskVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Flowable 任务 ID */
    private String taskId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dishId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long kitchenId;
    private String dishName;
    private String kitchenName;
    private String processInstanceId;
}
