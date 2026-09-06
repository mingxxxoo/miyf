package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.Instant;
import java.util.List;

/**
 * 预约单展示 VO。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "OrderVo", description = "预约单")
public class OrderVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "预约 ID", type = "string")
    private Long id;

    @Schema(description = "预约单号")
    private String orderNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户 ID", type = "string")
    private Long userId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "整单备注")
    private String remark;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "更新时间")
    private Instant updatedAt;

    @Schema(description = "明细")
    private List<OrderItemVo> items;
}
